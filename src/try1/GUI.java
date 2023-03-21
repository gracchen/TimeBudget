package try1;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class GUI extends JFrame {
	private List<LocalDate> week;
	private List<Double> budget;
	private JLabel msg;
	private static final long serialVersionUID = 1L;
	private File constFile, paramFile;
	private double consts[];
	private double daily;
	private DateTimeFormatter formatter;
	public GUI () {
		super("TimeBudget");
		pack();
		setLocationRelativeTo(null);
		setLayout(new FlowLayout());
		constFile = new File("constants.txt");
		paramFile = new File("params.txt");
		consts = new double[7];
		daily = 0;
		msg = new JLabel("hi");
		add(msg);
		week = new LinkedList<LocalDate>();
		budget = new ArrayList<Double>(Collections.nCopies(7,24.0));
		formatter = DateTimeFormatter.ofPattern("MMMM dd yyyy");
		readConstants();
		readParams();
		
		LocalDate curr = LocalDate.now();
		for (int i = 0; i < 7; i++,curr = curr.plusDays(1))
		{
			week.add(curr);
			//System.out.println(consts[dayToIndex(curr.getDayOfWeek())]);
			budget.set(i, budget.get(i) - daily - consts[dayToIndex(curr.getDayOfWeek())]);
			System.out.println(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(curr) + ":" + curr.getDayOfWeek() + ":" + budget.get(i));
		}

	}
	
	private int dayToIndex(DayOfWeek x) {
		switch (x) { 
			case MONDAY:
				return 0;
			case TUESDAY:
				return 1;
			case WEDNESDAY:
				return 2;
			case THURSDAY:
				return 3;
			case FRIDAY:
				return 4;
			case SATURDAY:
				return 5;
			case SUNDAY:
				return 6;
		};
		return -1;
	}
	private void readConstants() {
		Scanner getX = null;
		try {
			getX = new Scanner(constFile);
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }
		if (getX != null)
		{
			for (int i = 0; i < 7 && getX.hasNextLine(); i++)
			{
				double total = 0;
				String temp[] = getX.nextLine().split(",");
				for (int j = 0; j < temp.length; j++)
				{
					try {
						total += Double.valueOf(temp[j]);
					} catch (Exception e) {  };
				}
				//System.out.println(total);
				consts[i] = total;
			}
			getX.close();
			return;
		}
		return;
	}
	private void readParams() {
		Scanner getX = null;
		try {
			getX = new Scanner(paramFile);
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }
		if (getX != null)
		{
			while(getX.hasNextLine())
			{
				String temp[] = getX.nextLine().split(",");
				System.out.println(temp[0]);
				try {
					daily += Double.valueOf(temp[1]);
				} catch (Exception e) {  };
			}
			getX.close();
			//System.out.println("daily " + daily);
			return;
		}
		
		return;
	}
}
