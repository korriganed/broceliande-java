package net.adrouet.broceliande.algo;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.adrouet.broceliande.data.TestData;
import net.adrouet.broceliande.data.TestDataSet;
import net.adrouet.broceliande.struct.IData;

import static org.junit.Assert.*;

public class SplitterTest {

	@Test
	public void findBestSplitSimpleOrdered() throws Exception {
		List<IData> datas = new ArrayList<>();
		datas.add(new TestData("M", 1, "YES"));
		datas.add(new TestData("M", 2, "NO"));

		List<IData> datasCopy = new ArrayList<>(datas);
		TestDataSet testDataSet = new TestDataSet(datasCopy);

		BestSplit bestSplit = Splitter.findBestSplit(testDataSet, TestData.class.getMethod("getAge"));

		assertTrue(bestSplit.getCutPoint().test(datas.get(0)));
		assertFalse(bestSplit.getCutPoint().test(datas.get(1)));
		assertEquals(TestData.class.getMethod("getAge"), bestSplit.getFeature());
	}

    @Test
    public void findBestSplitOrderedNoSplit() throws Exception {
        List<IData> datas = new ArrayList<>();
        datas.add(new TestData("M", 1, "YES"));
        datas.add(new TestData("M", 1, "NO"));

        TestDataSet testDataSet = new TestDataSet(datas);

        BestSplit bestSplit = Splitter.findBestSplit(testDataSet, TestData.class.getMethod("getAge"));

        assertNull(bestSplit.getCutPoint());
    }

	@Test
	public void findBestSplitSimpleCategorical() throws Exception {
		List<IData> datas = new ArrayList<>();
		datas.add(new TestData("M", 1, "YES"));
		datas.add(new TestData("F", 1, "NO"));

		List<IData> datasCopy = new ArrayList<>(datas);
		TestDataSet testDataSet = new TestDataSet(datasCopy);

		BestSplit bestSplit = Splitter.findBestSplit(testDataSet, TestData.class.getMethod("getGender"));

		assertNotEquals(bestSplit.getCutPoint().test(datas.get(0)),
				bestSplit.getCutPoint().test(datas.get(1)));
		assertEquals(TestData.class.getMethod("getGender"), bestSplit.getFeature());

		datas.add(new TestData("F", 1, "YES"));
		datas.add(new TestData("F", 1, "NO"));
		testDataSet = new TestDataSet(datasCopy);

		bestSplit = Splitter.findBestSplit(testDataSet, TestData.class.getMethod("getGender"));

		assertTrue(bestSplit.getCutPoint().test(datas.get(0)));
		assertFalse(bestSplit.getCutPoint().test(datas.get(1)));
		assertFalse(bestSplit.getCutPoint().test(datas.get(2)));
		assertFalse(bestSplit.getCutPoint().test(datas.get(3)));
		assertEquals(TestData.class.getMethod("getGender"), bestSplit.getFeature());
	}

    @Test
    public void findBestSplitCategoricalNoSplit() throws Exception {
        List<IData> datas = new ArrayList<>();
        datas.add(new TestData("M", 1, "YES"));
        datas.add(new TestData("M", 1, "NO"));

        TestDataSet testDataSet = new TestDataSet(datas);

        BestSplit bestSplit = Splitter.findBestSplit(testDataSet, TestData.class.getMethod("getGender"));

        assertNull(bestSplit.getCutPoint());
    }

    @Test
	public void findBestSplitFeatureChoice() throws Exception {
		List<IData> datas = new ArrayList<>();
		datas.add(new TestData("M", 1, "YES"));
		datas.add(new TestData("F", 1, "NO"));
		datas.add(new TestData("F", 1, "NO"));

		List<IData> datasCopy = new ArrayList<>(datas);
		TestDataSet testDataSet = new TestDataSet(datasCopy);

		BestSplit bestSplit = Splitter.findBestSplit(testDataSet);

		assertEquals(TestData.class.getMethod("getGender"), bestSplit.getFeature());

		datas = new ArrayList<>();
		datas.add(new TestData("F", 1, "YES"));
		datas.add(new TestData("F", 2, "NO"));

		datasCopy = new ArrayList<>(datas);
		testDataSet = new TestDataSet(datasCopy);

		bestSplit = Splitter.findBestSplit(testDataSet);

		assertEquals(TestData.class.getMethod("getAge"), bestSplit.getFeature());
	}

}