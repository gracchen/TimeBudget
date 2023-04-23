package try1;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class GUI extends JFrame {
	private String[] workColNames = {"id", "name", "deadline", "hr", "diff", "fixed"};
	private String[] reviewColNames = {"id", "classID", "lectID", "lecture", "deadline", "hr", "isDone"};
	private JPanel home, tasks, settings;
	private List<LocalDate> week;
	private List<Double> breakBudget, budget, hrsLeft; //break vs non-break budget
	private List<List<SimpleEntry<Integer, Double>>> assign; //id + length
	private JTextField msg;
	private static final long serialVersionUID = 1L;
	private File dayOfWeekConstFile, dailyConstFile, reviewClassesFile;
	private double consts[], daily;
	private DateTimeFormatter formatter;
	private JButton toggleBreak;
	private boolean onBreak;
	private int weekDayIdeal = 1, weekEndIdeal = 6, oldWorkSize;
	private List<String> dateChoices;
	private JComboBox<String> drop;
	private JPanel show;
	private JLabel stats, leet, play;
	private List<SimpleEntry<JCheckBox, Integer>> checks;
	private JTabbedPane tabPane;
	private LocalDate week1 = LocalDate.of(2023, 4, 3); //spring quarter instruction starts April 3, this is dummy test var
	private JTable workTable, reviewTable; private JScrollPane js1, js2;
	private boolean editTasks = false;
	private List<Integer> tasksOrder; //changes which work indexes to show first depending on user's sort selection
	private JLabel clickCol;
	private JPanel taskToolBar; private JButton delete, add;
	private double hrsOnHwToday;
	private LocalDate today;
	private Connection con;
	private Statement st;
	private ResultSet rs;
	private String tableName = "time";
	private String reviewTableName = "review";
	private WorkTable workModel;
	private ReviewTable reviewModel;
	private JPanel newEntry; private JTextField nameField, hrField, deadlineField, diffField, hrsSpentField; 
	public GUI () {
		super("TimeBudget");
		pack();
		setLocationRelativeTo(null);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});

		//Connection: 
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			con=DriverManager.getConnection(
					"jdbc:mysql://upw0dxpo8jbhxutr:5sbWqBapPMO5lAaznMG6@byiig0vngbvcenz9feed-mysql.services.clever-cloud.com:3306/byiig0vngbvcenz9feed", "upw0dxpo8jbhxutr", "5sbWqBapPMO5lAaznMG6");
			st=con.createStatement();
		} catch (ClassNotFoundException e1) {e1.printStackTrace();} catch (SQLException e1) {e1.printStackTrace();}

		today = LocalDate.now();
		home = new JPanel();
		add(home);
		home.setLayout(new FlowLayout());
		tasks = new JPanel();
		settings = new JPanel();

		tabPane = new JTabbedPane();
		tabPane.addTab("Home", home);
		tabPane.addTab("Tasks", tasks);
		tabPane.addTab("Settings", settings);
		add(tabPane);
		ChangeListener changeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {

				System.out.println("Tab changed to: " + tabPane.getSelectedIndex());
				if (tabPane.getSelectedIndex() == 0) {
					if (editTasks) { 
						System.out.println("need to reschedule");
						editTasks = false;
						//workScheduler(); //recalculate once detect edits made + switch back to home
					}
					showSelected(drop.getSelectedIndex()); //refresh
				}
			}
		};
		tabPane.addChangeListener(changeListener);

		onBreak = false; //default break mode
		formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
		dayOfWeekConstFile = new File("constantsbyDayOfWeek.txt");
		dailyConstFile = new File("dailyConstants.txt");
		reviewClassesFile = new File("reviewClasses.txt");
		consts = new double[7];
		daily = 0;

		week = new LinkedList<LocalDate>();
		budget = new ArrayList<Double>(Collections.nCopies(7,24.0));
		hrsOnHwToday = 0.0;
		readDayOfWeekConstants();
		readDailyConsts();
		//-readSchoolWork();
		
		getHrsOnHwToday();
		LocalDate curr = today;
		double weekendBudget = 0.0;
		dateChoices = new ArrayList<String>();
		dateChoices.add("Today");
		dateChoices.add("Tomorrow");
		for (int i = 0; i < 7; i++, curr = curr.plusDays(1))
		{
			week.add(curr);
			budget.set(i, budget.get(i) - daily - consts[dayToIndex(curr)]);
			if (curr.getDayOfWeek() == DayOfWeek.SATURDAY) weekendBudget = budget.get(i);
			if (i >= 2) dateChoices.add((curr.getDayOfWeek()) + ", " + formatter.format(curr));
		}

		breakBudget = new ArrayList<Double>(Collections.nCopies(7,weekendBudget));
		addReviewToDo();
		//workScheduler();

		//GUI PART!!!msg = new JLabel("hi");
		msg = new JTextField();
		home.add(msg);
		msg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				try {
					hrsOnHwToday = Double.valueOf(event.getActionCommand());
					//workScheduler();
					showSelected(drop.getSelectedIndex()); //refresh
				} catch (Exception e) {System.out.println("invalid hrs spent today input");};
			}
		});

		toggleBreak = new JButton("off break");
		toggleBreak.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						onBreak = !onBreak; //toggle bool
						System.out.println(onBreak);
						toggleBreak.setText(onBreak? "on break" : "off break");
						//workScheduler();
						printBudget();
						printAssigned();
						printWork();
						showSelected(drop.getSelectedIndex()); //update
					}
				}
				);

		home.add(toggleBreak);
		//DROPDOWN GUI:
		drop = new JComboBox<String>(dateChoices.toArray(new String[dateChoices.size()])); //param = array of options
		home.add(drop);
		//showSelected(0); //default show today
		drop.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent event) {
						if(event.getStateChange() == ItemEvent.SELECTED) {
							showSelected(drop.getSelectedIndex());
						}
					}
				}
				);

		//TASKS = two panels, toolbar and workTable itself
		//toolbar:
		taskToolBar = new JPanel(); 
		clickCol = new JLabel("Click col workHeader to sort");
		taskToolBar.add(clickCol);
		delete = new JButton("Delete selected");
		delete.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						int[] select = workTable.getSelectedRows(); //takes selected items

						System.out.print("mark done ");
						for (int i = 0; i < select.length; i++) {
							System.out.print("work[" + tasksOrder.get(select[i]) + "], ");
						}
						workModel.removeRows(workTable.getSelectedRows());
						System.out.println("");
					}
				}
				);
		add = new JButton("+");
		taskToolBar.add(delete);
		taskToolBar.add(add);
		
		tasks.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		//workTable:
		gbc.gridx = 0; gbc.gridy = 0;
		workModel = new WorkTable();
		workTable = new JTable(workModel);
		workModel.loadTable();
		
		workTable.getTableHeader().setReorderingAllowed(false);
		
		tasks.add(new JScrollPane(workTable), gbc);
		/*
		 * workTable.getColumnModel().getColumn(1).setPreferredWidth(30);
		 * workTable.getColumnModel().getColumn(2).setPreferredWidth(10);
		 * workTable.getColumnModel().getColumn(3).setPreferredWidth(10);
		 * workTable.getColumnModel().getColumn(4).setPreferredWidth(10);
		 */
		
		reviewModel = new ReviewTable();
		reviewTable = new JTable(reviewModel);
		reviewModel.loadTable();
		reviewTable.getTableHeader().setReorderingAllowed(false);
		gbc.gridx = 1;
		tasks.add(new JScrollPane(reviewTable),gbc);
		
		//sort upon click
		JTableHeader workHeader = workTable.getTableHeader();
		workHeader.addMouseListener(new WorkTableHeaderMouseListener());
		JTableHeader reviewHeader = reviewTable.getTableHeader();
		reviewHeader.addMouseListener(new ReviewTableHeaderMouseListener());

		//workTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		System.out.println("\nNow Printing Work[]: ");
		printWork();

		//new entry window
		newEntry = new JPanel(); 
		nameField = new JTextField("Enter name");
		diffField = new JTextField("Enter diff");
		newEntry.add(nameField);
		newEntry.add(diffField);
		int result = JOptionPane.showConfirmDialog(null, newEntry, 
				"Please Enter X and Y Values", JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			System.out.println("x value: " + nameField.getText());
			System.out.println("y value: " + diffField.getText());
		}
	}

	public class WorkTableHeaderMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent event) {
			Point point = event.getPoint();
			int column = workTable.columnAtPoint(point);
			System.out.println("Header " + column + " clicked");
			workModel.loadTable(column); //workColNames[column]
			repaint(); //refresh
		}
	}
	
	public class ReviewTableHeaderMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent event) {
			Point point = event.getPoint();
			int column = workTable.columnAtPoint(point);
			System.out.println("Header " + column + " clicked");
			reviewModel.loadTable(column); //workColNames[column]
			repaint(); //refresh
		}
	}

	private class WorkTable extends DefaultTableModel {
		public void removeRows(int[] rows) 
		{
			for (int i = 0; i < rows.length; i++)
				removeRow(rows[i]);
		}
		public void loadTable() {
			setRowCount(0);
			runSQL("select * from " + tableName + ";", false);
			try {
				while(rs.next())
					workModel.addRow(new Object[]{rs.getInt("id"),rs.getString("name"), rs.getDate("deadline"), rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed")});
				//System.out.printf("%s:%s:%s\n", rs.getString("id"), rs.getString("name"), rs.getString("fixed"));
				System.out.println("successfully imported mySQL workTable to JTable");
			} catch (SQLException e) {e.printStackTrace(); System.err.println("load workTable failed");}
		}
		public void loadTable(int sortByCol) {
			setRowCount(0);
			runSQL("select * from " + tableName + " order by " + workColNames[sortByCol] + ";", false);
			try {
				while(rs.next())
					workModel.addRow(new Object[]{rs.getInt("id"),rs.getString("name"), rs.getDate("deadline"), rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed")});
				//System.out.printf("%s:%s:%s\n", rs.getString("id"), rs.getString("name"), rs.getString("fixed"));
				System.out.println("successfully imported mySQL workTable to JTable");
			} catch (SQLException e) {e.printStackTrace(); System.err.println("load workTable failed");}
		}
		public Class getColumnClass(int column) {
			switch (column) {
			case 0:	//id
				return Integer.class;
			case 1:	//name
				return String.class;
			case 2:	//deadline
				return Date.class;
			case 3:	//hr
				return Double.class;
			case 4:	//diff
				return Integer.class;
			default://fixed
				return Boolean.class;
			}
		}
		private static final long serialVersionUID = 1L;
		public String getColumnName(int col) {
			switch(col) {
			case 0: return "ID";
			case 1: return "Name";
			case 2: return "Deadline";
			case 3: return "Hr";
			case 4: return "Diff";
			default: return "Fixed";
			}
		}
		public int getColumnCount() { return 6; }
	}
	
	private class ReviewTable extends DefaultTableModel {
		public void removeRows(int[] rows) 
		{
			for (int i = 0; i < rows.length; i++) removeRow(rows[i]);
		}
		public void loadTable(int sortByCol) {
			runSQL("select * from " + reviewTableName + " order by " + reviewColNames[sortByCol] + ";", false);
			setRowCount(0);
			try {
				while(rs.next())
					reviewModel.addRow(new Object[]{rs.getInt("id"),rs.getInt("classID"), rs.getDouble("lectID"), rs.getDate("lecture"), rs.getDate("deadline"), rs.getDouble("hr"), rs.getBoolean("isDone")});
				//System.out.printf("%s:%s:%s\n", rs.getString("id"), rs.getString("name"), rs.getString("fixed"));
				System.out.println("successfully imported mySQL review workTable to JTable");
			} catch (SQLException e) {e.printStackTrace(); System.err.println("load review workTable failed");}
		}
		public void loadTable() {
			setRowCount(0);
			runSQL("select * from " + reviewTableName + ";", false);
			try {
				while(rs.next())
					reviewModel.addRow(new Object[]{rs.getInt("id"),rs.getInt("classID"), rs.getDouble("lectID"), rs.getDate("lecture"), rs.getDate("deadline"), rs.getDouble("hr"), rs.getBoolean("isDone")});
				//System.out.printf("%s:%s:%s\n", rs.getString("id"), rs.getString("name"), rs.getString("fixed"));
				System.out.println("successfully imported mySQL review workTable to JTable");
			} catch (SQLException e) {e.printStackTrace(); System.err.println("load review workTable failed");}
		}

		public Class getColumnClass(int column) {
			switch (column) {
			case 0:	//id
			case 1:	//classID
				return Integer.class;
			case 2:	//lectID
				return Double.class;
			case 3:	//lecture
				return Date.class;
			case 4:	//deadline
				return Date.class;
			case 5:	//hr
				return Double.class;
			default://isDone
				return Boolean.class;
			}
		}
		private static final long serialVersionUID = 1L;
		public String getColumnName(int col) {
			switch(col) {
			case 0: return "id";
			case 1: return "classID";
			case 2: return "lectID";
			case 3: return "lecture";
			case 4: return "deadline";
			case 5: return "hr";
			default: return "isDone";
			}
		}
		public int getColumnCount() { return 7; }
	}

	void showSelected(int index) { 
		//shows both leetcode + play and assigned stuff
		msg.setText(String.valueOf(hrsOnHwToday));
		if (show != null) {
			show.removeAll(); //remove previous display
			remove(show);
		}
		else show = new JPanel();
		GridBagConstraints c = new GridBagConstraints();
		show.setLayout(new GridBagLayout());

		home.add(show);

		/*		class checkHandler implements ItemListener { 
			public void itemStateChanged(ItemEvent event) { 
				for (int i = 0; i < checks.size(); ) {
					if (checks.get(i).getKey().isSelected()) {
						int curr = checks.get(i).getValue();

						hrsOnHwToday += work.get(curr).hr; //+= hr of checked
						msg.setText(String.valueOf(hrsOnHwToday));
						if (work.get(curr) instanceof DupeEntry) //currently checked off is dupe
						{
							int parent = ((DupeEntry)work.get(curr)).parent; //parent
							System.out.println("work["+curr+"] is dupe of work["+parent+"]");
							work.get(parent).hr -= work.get(curr).hr; //INSTEAD OF OVERCOMPLEX MERGING DUPES
							if (work.get(parent).hr <= 0) { //last dupe of entry
								if (work.get(parent) instanceof ReviewEntry) {
									int classIndex = ((ReviewEntry)work.get(parent)).classIndex;//mark parent review as doneReviews
									String temp[] = work.get(parent).name.split(" ");
									doneReviews.get(classIndex).add(temp[temp.length-1]); //get week#.lect# of name
									done.add(checks.get(i).getValue());
									System.out.println("\tfinished " + String.valueOf(temp[temp.length-1]) + " to class " + classIndex);
								}
								else done.add(parent); //if last dupe of non-review entry, also mark to remove upon exit
							}
						}
						else
						{
							if (work.get(curr) instanceof ReviewEntry)
							{
								System.out.println("work[" + checks.get(i).getValue() +"] is review");
								done.add(checks.get(i).getValue());
								int classIndex = ((ReviewEntry)work.get(curr)).classIndex; //mark review as doneReviews
								String temp[] = work.get(curr).name.split(" ");
								doneReviews.get(classIndex).add(temp[temp.length-1]); //get week#.lect# of name
								System.out.println("\tfinished " + String.valueOf(temp[temp.length-1]) + " to class " + classIndex);
							}
							else {
								System.out.println("work[" + curr +"] NOT review");
								done.add(curr); //if non-review entry, remove checked upon exit
							}
						}
						//unintended awesome design: don't have to edit work[ ] itself, messing up indexes. Simply
						//unassign from assign [ ], voila! 

						assign.get(drop.getSelectedIndex()).remove(checks.get(i).getValue()); //unassign checked item
						show.remove(checks.get(i).getKey());
						checks.remove(i);
						revalidate(); //to refresh removal
					}
					else i++;
				}
				workModel.loadTable();
			}
		}
		 */

		c.fill = GridBagConstraints.HORIZONTAL;    //fill entire cell with text to center
		c.gridwidth = 4; c.gridx = 0; c.gridy = 0;   //coords + width of msg element
		checks = new ArrayList<SimpleEntry<JCheckBox, Integer>>(); //handler needs to check if is ReviewEntry, so also store int (index to assign)
		//-checkHandler handler = new checkHandler();
		for (int i = 0; i < assign.get(index).size(); i++) {
			runSQL("select * from " + tableName + " where id = " + assign.get(index).get(i).getKey() + ";", false);
			Entry temp = null;
			try {
				if (rs.next()) {
					LocalDate dueDate = rs.getDate("deadline").toLocalDate();
					temp = new Entry(rs.getInt("id"), rs.getString("name"), dueDate, rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed"));
					JCheckBox b = new JCheckBox(temp.name + ",  " + temp.hr + "h");
					SimpleEntry<JCheckBox, Integer> a = new SimpleEntry<JCheckBox, Integer>(b, assign.get(index).get(i).getKey());
					checks.add(a);
					checks.get(checks.size()-1).getKey().setToolTipText("due " + formatter.format(temp.deadline));
					//System.out.println(":(" + checks.get(checks.size()-1).getKey().getText() + " " + checks.get(checks.size()-1).getValue());

					//-checks.get(checks.size()-1).getKey().addItemListener(handler);
					show.add(checks.get(checks.size()-1).getKey(), c);
					c.gridy++;
				}
			} catch (SQLException e) {e.printStackTrace();}

			
		}

		stats = new JLabel(hrsLeft.get(index) + "/" + (onBreak? breakBudget.get(index) : budget.get(index)) + "h free"); 
		show.add(stats, c);
		if (hrsLeft.get(index) > 0)
		{
			c.gridy++; //%.2f to format double show 2 decimal places max
			leet = new JLabel(formatDuration(hrsLeft.get(index) * 0.6) + " for CS");
			show.add(leet,c);
			c.gridy++;
			play = new JLabel(String.format("%.1fh to play", hrsLeft.get(index) * 0.4));
			play = new JLabel(formatDuration(hrsLeft.get(index) * 0.4) + " to play");
			show.add(play,c);
		}	

		revalidate();
	}

	String formatDuration(double x) {
		if (Math.ceil(x) == Math.floor(x)) //no decimal part
			return (String.format("%.0fh", x));
		return (String.format("%.0fh %.0fm", x, (x - Math.floor(x))*60));
	}

	int Ideal(LocalDate x) {
		int y = dayToIndex(x);
		if (y == 5 || y == 6) return weekEndIdeal;
		return weekDayIdeal;
	}

	void printWork() {
		workModel.loadTable();
	}

	void printBudget() {
		LocalDate curr = today;
		System.out.println("Date\tdayOfWeek  budget");
		if (onBreak) {
			for (int i = 0; i < 7; i++, curr = curr.plusDays(1))
				System.out.println(formatter.format(curr) + ":" + curr.getDayOfWeek() + ":" + breakBudget.get(i));
		}
		else {
			for (int i = 0; i < 7; i++, curr = curr.plusDays(1))
				System.out.println(formatter.format(curr) + ":" + curr.getDayOfWeek() + ":" + budget.get(i));
		}
	}

	void printAssigned() {
		System.out.println("printing dates, remaining budget out of max, assigned work");
		if (onBreak) {
			for (int i = 0; i < 7; i++)
			{
				System.out.print(formatter.format(week.get(i)) + ":" + week.get(i).getDayOfWeek() + " and # assigned: " + assign.get(i).size() + "\n\t" );
				for (int j = 0; j < assign.get(i).size(); j++) {
					System.out.println(assign.get(i).get(j).getKey() + " of len " + assign.get(i).get(j).getValue());
					runSQL("select * from " + tableName + " where id = " + assign.get(i).get(j).getKey() + ";", false);

					try {
						if (rs.next())
							System.out.printf("%s\t%s\t%s\t%s/%s\t%s\t%s\n",rs.getInt("id"),rs.getString("name"), rs.getDate("deadline"), assign.get(i).get(j).getValue(),rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed"));
					} catch (SQLException e) {e.printStackTrace();}
				}
				System.out.print("\n\t" + hrsLeft.get(i) + "h out of " + breakBudget.get(i) + "h free\n\n");
			}
		}
		else {
			for (int i = 0; i < 7; i++)
			{
				System.out.print(formatter.format(week.get(i)) + ":" + week.get(i).getDayOfWeek() + " and # assigned: " + assign.get(i).size() + "\n\t" );
				for (int j = 0; j < assign.get(i).size(); j++) {
					System.out.println(assign.get(i).get(j).getKey() + " of len " + assign.get(i).get(j).getValue());
					runSQL("select * from " + tableName + " where id = " + assign.get(i).get(j).getKey() + ";", false);

					try {
						if (rs.next())
							System.out.printf("%s\t%s\t%s\t%s/%s\t%s\t%s\n",rs.getInt("id"),rs.getString("name"), rs.getDate("deadline"), assign.get(i).get(j).getValue(),rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed"));
					} catch (SQLException e) {e.printStackTrace();}
				}
				System.out.print("\n\t" + hrsLeft.get(i) + "h out of " + budget.get(i) + "h free\n\n");
			}

		}
	}

	private void workScheduler() {	
		if (onBreak) hrsLeft = new ArrayList<Double>(breakBudget);
		else hrsLeft = new ArrayList<Double>(budget);
		hrsLeft.set(0, (hrsLeft.get(0) - hrsOnHwToday < 0)? 0 : hrsLeft.get(0) - hrsOnHwToday); 
		//that was inserted after, from indexes oldWorkSize, oldWorkSize+1......work.size()-1
		assign = new ArrayList<List<SimpleEntry<Integer, Double>>>(); //reset assign

		for (int i = 0; i < 7; i++)
		{
			ArrayList<SimpleEntry<Integer,Double>> temp = new ArrayList<SimpleEntry<Integer,Double>>();
			assign.add(temp);
		}

		runSQL("select * from " + tableName + " order by fixed desc, deadline, diff desc", false);
		/*
		 * 		try {
			while (rs.next())
				System.out.printf("%s\t%s\t%s\t%s\t%s\t%s\n",rs.getInt("id"),rs.getString("name"), rs.getDate("deadline"), rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

		//List<Integer> reviewToDo = new ArrayList<Integer>(); //make it very last priority
		try {
			while (rs.next()) {
				LocalDate dueDate = rs.getDate("deadline").toLocalDate();
				Entry temp = new Entry(rs.getInt("id"), rs.getString("name"), dueDate, rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed"));

				int id = rs.getInt("id");
				//if (work.get(i) instanceof ReviewEntry) reviewToDo.add(i); //deal with all review entries at end less priority than true deadlines
				//else 
				System.out.println("assignEntry(" + id + ", f);");
				assignEntry(temp, false);
			}
		} catch (SQLException e) {e.printStackTrace();}
		/*for (int i = 0; i < reviewToDo.size(); i++) {
			if (done.contains(reviewToDo.get(i))) continue;
			System.out.println("assignEntry(" + reviewToDo.get(i) + ", t);");
			assignEntry(reviewToDo.get(i), true);
		}*/

		printAssigned();
	}

	private void assignEntry(Entry curr, boolean isReview) { 
		int n = (int) ChronoUnit.DAYS.between(today, curr.deadline);
		if (!(n >= 0 && n < 7)) return;
		if (n<=0 && isReview) n = 8; //deadline passed, assign review to any day this week

		if (!isReview && curr.isFixed) 
		{
			assign.get((int) n).add(new SimpleEntry<Integer, Double>(curr.id, curr.hr)); //id of mysql entry assigned to date n's list
			hrsLeft.set((int) n, hrsLeft.get((int) n) - curr.hr);
		}
		else {
			if (n==0) n++;
			int idealN = -1;

			for (int j = 0; j < n-1; j++) //assign today? tmrw? day after? 
			{
				if (hrsLeft.get(j) - curr.hr >= 0)
				{
					if (idealN == -1) idealN = j;
					else {
						double id = hrsLeft.get(idealN) - curr.hr - Ideal(week.get(idealN));
						double now = hrsLeft.get(j) - curr.hr - Ideal(week.get(j)); //budget if assigned to j AND do ideal leet+play for that weekday/end
						if (now > id) idealN = j;
					}
				}
			}
			if (idealN == -1) //all dates before deadline no time even without leet+play
			{ //forced to assign assign the day of deadline

				double remainingUnassigned = curr.hr;
				if (hrsLeft.get((int) n-1) - curr.hr < 0) //if need to split bc deadline also not enough time
				{
					System.out.println("splitting " + curr.name);

					boolean doCont = true;
					if (isReview) n=8; //Review is soft deadline and no ideal days, so try fit all days before negative hours

					int sumAsIdeal = 0; //capacity of hrs til deadline if I follow through with ideal Leet/play
					for (int j = 0; j < n-1; j++) 
						sumAsIdeal += (hrsLeft.get(j) < Ideal(week.get(j)))? 0 : hrsLeft.get(j) - Ideal(week.get(j));

					if (sumAsIdeal >= curr.hr) { //best split case scenario, only take, at max, sweet free time out of each day
						System.out.println("best case!");
						for (int j = 0; j < n-1 && doCont; j++) //assign today? tmrw? day after? 
						{
							System.out.println("trying day " + j);
							double extraTime = hrsLeft.get(j) - Ideal(week.get(j));
							if (extraTime > 0) //has some extra time even w/ ideal leet+play
							{
								if (remainingUnassigned <= extraTime) { //doneReviews! all parts fitted
									doCont = false;
									assign.get(j).add(new SimpleEntry<Integer, Double> (curr.id, remainingUnassigned)); //direct assign to day j
									hrsLeft.set(j, hrsLeft.get(j) - remainingUnassigned); //update hours of day j
								}
								else { //gotta do more splitting, take entire extraTime
									assign.get(j).add(new SimpleEntry<Integer, Double>(curr.id, extraTime));
									remainingUnassigned -= extraTime;
									hrsLeft.set(j, (double) Ideal(week.get(j))); //which eats up all of day j's bonus hrs left.
								}
							}
						}
						if (doCont) //still need to assign remaining portion, last resort is to deadline
						{
							assign.get((int) n-1).add(new SimpleEntry<Integer, Double>(curr.id, remainingUnassigned)); //index of remaining portion assigned to date n's list
							hrsLeft.set((int) n-1, hrsLeft.get((int) n-1) - remainingUnassigned); //subtract remain portion time from deadline hrsleft
						}
					}
					else { //old alg:
						System.out.println("unfortunate split case");
						double takenLPTime = 0; //LP = leet + play time
						double needLPTTake = curr.hr - sumAsIdeal; //how much leet+play time must be taken total from day 0 to day b4 deadline
						for (int j = 0; j < n-1 && doCont; j++) //assign today? tmrw? day after? 
						{
							if (takenLPTime < needLPTTake) { //need to take all of today's time. 
								if (hrsLeft.get(j) > 0) //has some time to squeeze this task
								{
									if (remainingUnassigned <= hrsLeft.get(j)) { //all parts fitted
										doCont = false;
										assign.get(j).add(new SimpleEntry<Integer, Double>(curr.id, remainingUnassigned)); //direct assign to day j
										hrsLeft.set(j, hrsLeft.get(j) - remainingUnassigned); //update hours of day j
									}
									else { //gotta do more splitting, take entire day's time
										assign.get(j).add(new SimpleEntry<Integer, Double>(curr.id, hrsLeft.get(j)));
										remainingUnassigned -= hrsLeft.get(j);
										hrsLeft.set(j, 0.0); //which eats up all of day j's hrs left.
										takenLPTime += Ideal(week.get(j)); //now i used up this much of day j's ideal time, count in total
									}
								}
							}
							else { //now can afford to take just bonus time
								double extraTime = hrsLeft.get(j) - Ideal(week.get(j));
								if (extraTime > 0) //has some extra time even w/ ideal leet+play
								{
									if (remainingUnassigned <= extraTime) { //all parts fitted
										doCont = false;
										assign.get(j).add(new SimpleEntry<Integer,Double>(curr.id, remainingUnassigned)); //direct assign to day j
										hrsLeft.set(j, extraTime - remainingUnassigned); //update hours of day j
									}
									else { //gotta do more splitting. Greedy, take entire bonus time of day j
										assign.get(j).add(new SimpleEntry<Integer,Double>(curr.id, extraTime));
										remainingUnassigned -= extraTime; //update remaining portion of task
										hrsLeft.set(j, (double) Ideal(week.get(j))); //which eats up all of day j's hrs left.
									}
								}
							}
						}
						if (doCont) //still need to assign remaining portion, last resort is to deadline
						{
							assign.get((int) n-1).add(new SimpleEntry<Integer, Double>(curr.id, remainingUnassigned)); //index of remaining portion assigned to date n's list
							hrsLeft.set((int) n-1, hrsLeft.get((int) n-1) - remainingUnassigned); //subtract remain portion time from deadline hrsleft
						}
					}
				}
				else { //deadline only day enough time for entire assignment, so assign to deadline
					assign.get((int) n-1).add(new SimpleEntry<Integer, Double>(curr.id, curr.hr)); //index of remaining portion assigned to date n's list
					hrsLeft.set((int) n-1, hrsLeft.get((int) n-1) - curr.hr); //subtract remain portion time from deadline hrsleft
				}
			}
			else //ideal fits perfectly, just assign it to ideal day.
			{
				assign.get(idealN).add(new SimpleEntry<Integer, Double>(curr.id, curr.hr)); //index of work assigned to date idealN's list
				hrsLeft.set(idealN, hrsLeft.get(idealN) - curr.hr);
			}
		}
	}


	private int dayToIndex(LocalDate y) {
		return dayToIndex(y.getDayOfWeek());
	}

	private int dayToIndex(DayOfWeek y) {

		switch (y) { 
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

	private void getHrsOnHwToday() {
		hrsOnHwToday = 0.0;
		Scanner getX = null;
		try {
			getX = new Scanner(new File("todaySpent.txt"));
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }
		if (getX == null) return;
		try {
			if (LocalDate.parse(getX.nextLine(), formatter).isEqual(today))
			{
				try {
					hrsOnHwToday = Double.valueOf(getX.nextLine());
				} catch (Exception e) { System.out.println("invalid duration in todaySpent.txt"); }
			}
		} catch (Exception e) {System.out.println("invalid date in todaySpent.txt");}
		getX.close();
	}

	private void readDayOfWeekConstants() {
		Scanner getX = null;
		try {
			getX = new Scanner(dayOfWeekConstFile);
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

	private void readDailyConsts() {
		Scanner getX = null;
		try {
			getX = new Scanner(dailyConstFile);
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }
		if (getX != null)
		{
			while(getX.hasNextLine())
			{
				String temp[] = getX.nextLine().split(",");
				//System.out.println(temp[0]);
				try {
					daily += Double.valueOf(temp[1]);
				} catch (Exception e) {  };
			}
			getX.close();
			//System.out.println("daily " + daily);
			return;
		}
	}

	private int daysBetweenDayOfWeeks(String x, String y) { //crappy slow way of implementing this
		DayOfWeek a = initialDayOfWeek(x);
		DayOfWeek b = initialDayOfWeek(y);
		int A=0; 
		for (; today.plusDays(A).getDayOfWeek() != a; A++);
		int B=A;
		for (; today.plusDays(B).getDayOfWeek() != b; B++);
		//System.out.println("daysBetweenDayOfWeeks("+x+","+y+")="+(B-A));
		return (B-A);
	}

	private void addReviewToDo() {
		System.out.println("Generating Review Sessions...");
		boolean doGenerate = false;
		int res = runSQL("select * from " + reviewTableName + ";", false);
		if (res == -1) {
			System.out.println("whoops, review doesn't exist, let me create it");
			runSQL("create workTable " + reviewTableName + " (id int NOT NULL AUTO_INCREMENT, classID int(11), lectID double, lecture date, deadline date, hr double, isDone bit(1), primary key (id));", false);
			doGenerate = true;
		} else {
			try {
				if (!rs.next()) doGenerate = true; //empty
			} catch (SQLException e) {e.printStackTrace();}
		}
		if (!doGenerate) return; //reviews already generated, do nothing
		
		Scanner getX = null;
		try {
			getX = new Scanner(reviewClassesFile);
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }

		if (getX != null)
		{
			int classIndex = 0;
			while(getX.hasNextLine())
			{
				String line = getX.nextLine();
				String name = line.substring(1, line.indexOf("\"",1));
				line = line.substring(line.indexOf("\"", 1)+2); //remove name portion
				String temp1[] = line.split(",");

				for (int i = 1; i < temp1.length; i++) 
				{
					//first class at this day of week: week1.plusDays(dayToIndex(initialDayOfWeek(temp[i])));
					int nDeadline; //days til next deadline
					int n = dayToIndex(initialDayOfWeek(temp1[i])); //M = 0
					if (i+1 == temp1.length) nDeadline = daysBetweenDayOfWeeks(temp1[i], temp1[1]);
					else nDeadline  = daysBetweenDayOfWeeks(temp1[i], temp1[i+1]);

					for (int j = 0; j < 10; j++) { //10 weeks
						String lectID = (n/7+1)+"."+i; //week.lect#  i.e. 4.1 means 1st lecture of week 4
						runSQL(String.format("insert into %s (classID, lectID, lecture, deadline, hr, isDone) values (%d,%s, date '%s', date '%s', %s, 0)", 
								reviewTableName, classIndex, lectID, week1.plusDays(n), week1.plusDays(n+nDeadline), temp1[0]), false);
						n += 7; //check next week
					}
				}
				classIndex++;
			}
			getX.close();
		}
	}

	DayOfWeek initialDayOfWeek(String a) {
		switch(a) {
		case "M": 
			return DayOfWeek.MONDAY;
		case "T":
			return DayOfWeek.TUESDAY;
		case "W":
			return DayOfWeek.WEDNESDAY;
		case "R":
			return DayOfWeek.THURSDAY;
		default:
			return DayOfWeek.FRIDAY;
		}
	}

	void loadTable() {
		workModel.setRowCount(0); //clear the workTable
		runSQL("select * from " + tableName + " ", false);
		try {
			while(rs.next()) {
				workModel.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getDate("deadline"), rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed")});
				System.out.printf("%s\t%s\t%s\t%s\t%s\t%s\n", rs.getInt("id"), rs.getString("name"), rs.getDate("deadline"), rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed"));
			}
			System.out.println("successfully imported mySQL workTable to JTable");
		} catch (SQLException e) {e.printStackTrace(); System.err.println("B rs.next() failed");}
	}

	int runSQL(String query, boolean loadTable) {
		if (query.indexOf("select") == 0) {
			try {
				rs = st.executeQuery(query);
				System.out.println(query + " was successful");
				return 0;
			} catch (SQLException e) {e.printStackTrace(); System.err.println(query + " failed"); return -1;}
		}
		else {
			try {
				st.executeUpdate(query);
				System.out.println(query + " was successful");
				if (loadTable) workModel.loadTable();
				return 0;
			} catch (SQLException e) {e.printStackTrace(); System.err.println(query + " failed"); return -1;}
		}


	}

	private class Entry {
		int id;
		String name;
		LocalDate deadline;
		double hr;
		int diff;
		boolean isFixed;

		Entry(int i, String n, LocalDate de, double h, int di, boolean t) {
			id = i;
			name = n;
			deadline = de;
			hr = h;
			diff = di;
			isFixed = t;
		}

		public String toString() {
			return String.format("\"" + name + "\"\t" + diff + "\t" + hr + "\t" + formatter.format(deadline) + "\t" + isFixed);
		}
	}
}
