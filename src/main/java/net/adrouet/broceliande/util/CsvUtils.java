package net.adrouet.broceliande.util;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;

import net.adrouet.broceliande.bean.Passenger;
import net.adrouet.broceliande.struct.IData;

public class CsvUtils {

	public static List<IData> csvToPassager(String filename) throws IOException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		String file = classloader.getResource(filename).getFile();
		FileReader fReader = new FileReader(file);
		CSVReader reader = new CSVReader(fReader);
		String[] nextLine;
		ArrayList<IData> result = new ArrayList<>();
		reader.readNext(); // Ignore header

		while ((nextLine = reader.readNext()) != null) {
			Passenger p = new Passenger();
			p.setPassengerId(getInteger(nextLine[0]));
			p.setSurvived(getInteger(nextLine[1]));

			Integer c = getInteger(nextLine[2]);
			if (c == null) {
				p.setPclass(3);
			} else {
				p.setPclass(c);
			}

			p.setName(nextLine[3]);
			p.setSex(nextLine[4]);

			Integer age = getInteger(nextLine[5]);
			if (age == null) {
				p.setAge(23);
			} else {
				p.setAge(age);
			}

			p.setSibSp(getInteger(nextLine[6]));
			p.setParch(getInteger(nextLine[7]));
			p.setTicket(nextLine[8]);

			Double fare = getDouble(nextLine[9]);
			if (fare == null) {
				p.setFare(8.05);
			} else {
				p.setFare(fare);
			}

			p.setCabin(nextLine[10]);

			String embarked = nextLine[11];
			if ((embarked == null) || (embarked.length() == 0)) {
				p.setEmbarked("S");
			} else {
				p.setEmbarked(embarked);
			}
			result.add(p);
		}
		return result;
	}

	private static Integer getInteger(String s) {
		if (s == null || s.isEmpty()) {
			return null;
		}
		try {
			return Integer.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Double getDouble(String s) {
		if (s == null || s.isEmpty()) {
			return null;
		}
		try {
			return Double.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
