package net.adrouet.broceliande.data;

import net.adrouet.broceliande.struct.IData;
import net.adrouet.broceliande.struct.DataSet;
import net.adrouet.broceliande.util.InspectionUtils;

import java.beans.IntrospectionException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TestDataSet implements DataSet {

	private List<IData> data;
	private Set<Comparable> j;

	public TestDataSet() {

	}

	public TestDataSet(List<IData> data) {
		setData(data);
	}

	@Override
	public void setData(List<IData> data) {
		this.data = data;
		j = data.stream().map(IData::getResult).distinct().collect(Collectors.toSet());
	}

	@Override
	public Set<Comparable> getJ() {
		return j;
	}

	@Override
	public Set<Method> getP() {
		try {
			return InspectionUtils.findFeatures(TestData.class);
		} catch (IntrospectionException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<IData> getL_t() {
		return data;
	}

}
