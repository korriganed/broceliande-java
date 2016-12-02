package net.korriganed.broceliande.algo;

import static net.korriganed.broceliande.util.InspectionUtils.invokeGetter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.korriganed.broceliande.data.FeatureType;
import net.korriganed.broceliande.struct.DataSet;
import net.korriganed.broceliande.struct.Occurrences;
import net.korriganed.broceliande.util.InspectionUtils;

public class Splitter<D, R> {

	private static final Logger LOG = LoggerFactory.getLogger(Splitter.class);

	private Integer k;
	private Random random;

	public Splitter() {
		this(null, new Random());
	}

	public Splitter(Integer k, Random random) {
		this.k = k;
		this.random = random;
	}

	public BestSplit<D> findBestSplit(DataSet<D, R> dataSet) {
		if (this.k == null) {
			this.k = dataSet.getFeatureGetters().size();
		}

		BestSplit<D> bestSplit = new BestSplit<>(null, null, 0.);
		Double delta = Double.NEGATIVE_INFINITY;
		ArrayList<Method> randomFeatureGetters = new ArrayList<>(dataSet.getFeatureGetters());
		// #1 draw random getters from the features
		Collections.shuffle(randomFeatureGetters, this.random);
		while (randomFeatureGetters.size() > this.k) {
			randomFeatureGetters.remove(randomFeatureGetters.size() - 1);
		}
		for (Method featureGetter : randomFeatureGetters) {
			// #1 find the best binary split s*_j defined on X_j
			BestSplit<D> bestSplitOfFeature = this.findBestSplit(dataSet, featureGetter);
			if (bestSplitOfFeature.getImpurityDecrease() > delta) {
				delta = bestSplitOfFeature.getImpurityDecrease();
				bestSplit = bestSplitOfFeature;
			}
		}
		LOG.trace("Best split found: {}", bestSplit);

		return bestSplit;
	}

	/**
	 * See page 50 of LOUPPE's thesis about random forests
	 *
	 * @param dataSet
	 *            the subset of node samples falling into node t (contains L_t)
	 * @param featureGetter
	 *            the j-th input variable or feature (getter)
	 */
	public BestSplit<D> findBestSplit(DataSet<D, R> dataSet, Method featureGetter) {
		BestSplit<D> bestSplitOfFeature = new BestSplit<>(featureGetter, null, 0.);
		Double delta = 0.;

		// #1 initialize the statistics for t_R to i(t)
		// t_R: the right child of node t
		Occurrences<D, R> occ_R = new Occurrences<>(dataSet);
		// #A fill the right node with all data
		dataSet.getSample().forEach(occ_R::add);

		// #2 initialize the statistics for t_L to 0
		// t_L: the left child of node t
		Occurrences<D, R> occ_L = new Occurrences<>(dataSet);

		// #3 initialize i(t)
		// i(t): the impurity of node t
		Double it = impurityG(occ_R.getOccurrences(), dataSet.getSample().size());

		List<D> sample = dataSet.getSample();
		Integer sampleSize = sample.size();
		if (InspectionUtils.getFeatureType(featureGetter).equals(FeatureType.CATEGORICAL)) {
			Stream<List<D>> sampleGroupByFeatureValue = sample.stream().map(data -> invokeGetter(data, featureGetter))
					.distinct()
					.map(featureValue -> sample.stream()
							.filter(data -> invokeGetter(data, featureGetter).equals(featureValue))
							.collect(Collectors.toList()));

			BestSplit<D> bestSplit = sampleGroupByFeatureValue.map(group -> {
				Occurrences<D, R> right = new Occurrences<>(dataSet);
				dataSet.getSample().forEach(right::add);
				Occurrences<D, R> left = new Occurrences<>(dataSet);
				group.forEach(d -> {
					right.remove(d);
					left.add(d);
				});
				Double p_L = left.getTotal() / ((double) sample.size());
				Double p_R = right.getTotal() / ((double) sample.size());
				Double impurityDecrease = it - p_L * impurityG(left.getOccurrences(), left.getTotal())
						- p_R * impurityG(right.getOccurrences(), right.getTotal());
				D d = group.get(0);
				Predicate<D> p = toCheck -> invokeGetter(toCheck, featureGetter).equals(invokeGetter(d, featureGetter));
				BestSplit<D> split = new BestSplit<>(featureGetter, p, impurityDecrease);
				return split;
			}).max(Comparator.comparing(BestSplit::getImpurityDecrease)).get();

			return bestSplit;
		} else {
			// #4 sort the sample L_t using the comparator on X_j returned
			// values
			Comparator<D> comparator = (x1, x2) -> InspectionUtils.<Comparable> invokeGetter(x1, featureGetter)
					.compareTo(InspectionUtils.<Comparable> invokeGetter(x2, featureGetter));
			Collections.sort(dataSet.getSample(), comparator);

			// #5 loop over the sample of t
			int i = 0; // /!\ starts at 1 in the algorithm of the thesis
			while (i < sampleSize) {
				while (((i + 1) < sampleSize) && (comparator.compare(sample.get(i), sample.get(i + 1)) == 0)) {
					occ_L.add(sample.get(i));
					occ_R.remove(sample.get(i));
					++i;
				}
				occ_L.add(sample.get(i));
				occ_R.remove(sample.get(i));
				++i;
				if (i < sampleSize) {
					// ∆i(s, t): the impurity decrease of split s at node t
					// ∆i(s, t) = i(t) - p_L i(t_L) - p_R i(t_R)
					Double p_L = occ_L.getTotal() / ((double) sample.size());
					Double p_R = occ_R.getTotal() / ((double) sample.size());
					Double impurityDecrease = it - p_L * impurityG(occ_L.getOccurrences(), occ_L.getTotal())
							- p_R * impurityG(occ_R.getOccurrences(), occ_R.getTotal());

					Predicate<D> p;
					if (InspectionUtils.getFeatureType(featureGetter).equals(FeatureType.CATEGORICAL)) {
						D d = sample.get(i);
						p = data -> comparator.compare(data, d) == 0;
						occ_R.addFrom(occ_L);
						occ_L.reset();
					} else {
						// v'_k: the mid-cut point between v_k and v_k+1
						Double midCutPoint = average(
								InspectionUtils.<Number> invokeGetter(sample.get(i), featureGetter),
								InspectionUtils.<Number> invokeGetter(sample.get(i - 1), featureGetter));
						p = data -> (InspectionUtils.<Number> invokeGetter(data, featureGetter))
								.doubleValue() < midCutPoint;
					}

					if (impurityDecrease > delta) {
						delta = impurityDecrease;
						bestSplitOfFeature = new BestSplit<>(featureGetter, p, impurityDecrease);
					}
				}
			}
		}

		return bestSplitOfFeature;

	}

	private static Double average(Number a, Number b) {
		return (a.doubleValue() + b.doubleValue()) / 2;
	}

	/**
	 * The impurity function iG(t) based on the Gini index
	 *
	 * @param subSampleSizes
	 * @param totalSampleSize
	 * @return
	 */
	private static Double impurityG(List<Integer> subSampleSizes, Integer totalSampleSize) {
		Double impurity = 0.;
		for (Integer subSampleSize : subSampleSizes) {
			Double percentage = subSampleSize / (totalSampleSize.doubleValue());
			impurity += percentage * (1 - percentage);
		}
		return impurity;
	}

	/**
	 * The impurity function i_R(t) based on the local resubstitution estimate
	 * defined on the squared error loss
	 *
	 * @return
	 */
	private Double impurityR(DataSet<D, R> dataSet) {
		Integer N_t = dataSet.getSample().size();
		Double sum = 0.;
		Double impurity = 0.;
		for (D x : dataSet.getSample()) {
			// (TODO) XXX Ugly as hell
			sum = sum + ((Number) InspectionUtils.invokeGetter(x, dataSet.getTargetGetter())).doubleValue();
		}
		Double average = sum / N_t.doubleValue();
		for (D x : dataSet.getSample()) {
			impurity += Math.pow(
					((Number) InspectionUtils.invokeGetter(x, dataSet.getTargetGetter())).doubleValue() - average, 2);
		}
		impurity = impurity / N_t.doubleValue();
		return impurity;
	}

	/**
	 * The impurity function i_H(t) based on the Shannon entropy
	 *
	 * @return
	 */
	private Double impurityH(List<Integer> N_cts, Integer N_t) {
		Double it = 0.;
		for (Integer N_ct : N_cts) {
			Double pc_kt = N_ct / (N_t.doubleValue());
			it -= pc_kt * Math.log(pc_kt) / Math.log(2.);
		}
		return it;
	}
}
