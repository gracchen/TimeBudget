package try1;
import javafx.scene.control.CheckBox;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;

import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class GUI extends Application {
	private String[] workColNames = {"id", "name", "deadline", "hr", "diff", "fixed"};
	private String[] reviewColNames = {"id", "classID", "lectID", "lecture", "deadline", "hr", "isDone"};
	//	private JPanel home, tasks, settings;
	private FlowPane home;
	GridPane tasks;
	private FlowPane settings;
	private List<LocalDate> week;
	private List<Double> breakBudget, budget, hrsLeft; //break vs non-break budget
	private List<List<SimpleEntry<Integer, Double>>> assignWork; //id + length
	private List<List<SimpleEntry<Integer, Double>>> assignReview; //id + length
	private TextField msg;
	private static final long serialVersionUID = 1L;
	private File dayOfWeekConstFile, dailyConstFile, reviewClassesFile;
	private double consts[], daily;
	private DateTimeFormatter formatter;
	private Button toggleBreak;
	private boolean onBreak;
	private int weekDayIdeal = 1, weekEndIdeal = 6;
	private List<String> dateChoices;
	private ComboBox<String> drop;
	private GridPane show;
	private Label stats;
	Label play;
	Label leet;
	List<SimpleEntry<CheckBox, Integer>> workChecks;
	private List<SimpleEntry<CheckBox, Integer>> reviewChecks;
	private TabPane tabPane;
	private LocalDate week1 = LocalDate.of(2023, 4, 3); //spring quarter instruction starts April 3, this is dummy test var
	private JTable workTable, reviewTable; 
	private boolean editTasks = false;
	private Label clickCol;
	private FlowPane taskToolBar; Button delete;
	private Button add;
	private double hrsOnHwToday;
	private LocalDate today;
	private Connection con;
	private Statement st;
	private ResultSet rs;
	private String tableName = "time";
	private String reviewTableName = "review";
	private WorkTable workModel;
	private ReviewTable reviewModel;
	private boolean createPopupOpen = false;
	private int workTableSortedByCol = 0;
	private int reviewTableSortedByCol = 0;

	List<String> classNames = new LinkedList<String>();

	public static void main(String[] args) {
		launch(args);
	}

	public void start(Stage primaryStage) {

		//Connection: 
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			con=DriverManager.getConnection(
					"jdbc:mysql://upw0dxpo8jbhxutr:5sbWqBapPMO5lAaznMG6@byiig0vngbvcenz9feed-mysql.services.clever-cloud.com:3306/byiig0vngbvcenz9feed", "upw0dxpo8jbhxutr", "5sbWqBapPMO5lAaznMG6");
			st=con.createStatement();
		} catch (ClassNotFoundException e1) {e1.printStackTrace();} catch (SQLException e1) {e1.printStackTrace();}

		today = LocalDate.now();

		home = new FlowPane();
		tasks = new GridPane();
		settings = new FlowPane();

		tabPane = new TabPane();
		Tab homeTab = new Tab("Home", home);
		tabPane.getTabs().add(homeTab);
		tabPane.getTabs().add(new Tab("Tasks", tasks));
		tabPane.getTabs().add(new Tab("Settings", settings));

		homeTab.setOnSelectionChanged(e -> {
			if (homeTab.isSelected())
			{
				if (editTasks) { 
					System.out.println("need to reschedule");
					editTasks = false;
					workScheduler(); //recalculate once detect edits made + switch back to home
				}
				showSelected(drop.getSelectionModel().getSelectedIndex()); //refresh
			}
		}
				);

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
		workScheduler();

		//GUI PART!!!msg = new JLabel("hi");
		msg = new TextField();
		home.getChildren().add(msg);

		msg.setOnAction(event -> {
			try {
				hrsOnHwToday = Double.valueOf(msg.getText());
				workScheduler();
				showSelected(drop.getSelectionModel().getSelectedIndex()); // refresh
			} catch (NumberFormatException e) {
				System.out.println("Invalid hours spent today input");
			}
		});

		toggleBreak = new Button("off break");
		toggleBreak.setOnAction(event -> {
			onBreak = !onBreak; //toggle bool
			System.out.println(onBreak);
			toggleBreak.setText(onBreak? "on break" : "off break");
			workScheduler();
			printBudget();
			printAssigned();
			//printWork();
			showSelected(drop.getSelectionModel().getSelectedIndex()); //update
		}

				);

		home.getChildren().add(toggleBreak);
		//DROPDOWN GUI:

		drop = new ComboBox<String>();
		drop.setItems(FXCollections.observableArrayList(dateChoices));
		drop.setValue(drop.getItems().get(0)); // Set default value to index 0

		home.getChildren().add(drop);

		drop.setOnAction(event -> {
			showSelected(drop.getSelectionModel().getSelectedIndex());
		}
				);

		//TASKS = two panels, toolbar and workTable itself
		//toolbar:
		taskToolBar = new FlowPane(); 
		int gridx = 0; int gridy = 0;
		tasks.add(taskToolBar, gridx, gridy);
		clickCol = new Label("Click col workHeader to sort");
		delete = new Button("Delete selected");
		delete.setOnAction(event -> {
			workModel.removeRows(workTable.getSelectedRows());
		}
				);
		add = new Button("+");
		taskToolBar.getChildren().addAll(clickCol, delete, add);

		//new entry window
		GridPane newEntry = new GridPane(); 
		Scene scene1 = new Scene(newEntry, 400, 300);
		Stage stage1 = new Stage();
		stage1.setTitle("Create a new work entry");
		stage1.setScene(scene1);
		//stage1.show();

		TextField nField = new TextField();
		TextField deField = new TextField();
		TextField duField = new TextField();
		TextField diField = new TextField();
		TextField fField = new TextField();
		Button k = new Button("Create");
		nField.setText("Name");
		deField.setText("Deadline");
		duField.setText("Duration");
		diField.setText("Difficulty");
		fField.setText("Fixed?");

		k.setOnAction(e -> {
			if (nField.getText().indexOf("\"") != -1)
			{
				System.err.println("invalid name");
				return;
			}

			if (!(fField.getText().equals("0") || fField.getText().equals("1"))) {
				System.err.println("invalid boolean");
				return;
			}
			try {
				System.out.println(nField.getText());
				System.out.println(LocalDate.parse(deField.getText()));
				System.out.println(Double.valueOf(duField.getText()));
				System.out.println(Integer.valueOf(diField.getText()));
				System.out.println(fField.getText());
				runSQL(String.format("insert into %s (name, deadline, hr, diff, fixed) values (\"%s\",\'%s\',%s,%s,%s);", tableName, nField.getText(), deField.getText(), duField.getText(), diField.getText(), fField.getText()), true);
				nField.setText("Name");
				deField.setText("Deadline");
				duField.setText("Duration");
				diField.setText("Difficulty");
				fField.setText("Fixed?");
			} catch(Exception ed) {
				ed.printStackTrace(System.out);
				System.err.println("invalid date, please try again"); return;}

			createPopupOpen = false;
			stage1.hide();
			System.out.println("i am ded");

		}
				);

		//s.fill = GridBagConstraints.HORIZONTAL;
		taskToolBar.setHgap(10);
		taskToolBar.setVgap(30);
		taskToolBar.setPadding(new Insets(15,15,15,15));
		taskToolBar.setAlignment(Pos.TOP_CENTER);
		newEntry.add(nField, 0, 0);
		newEntry.add(deField, 0, 1);
		newEntry.add(duField, 0, 2);
		newEntry.add(diField, 1, 2);
		newEntry.add(fField, 2, 2);
		//s.fill = GridBagConstraints.NONE;
		newEntry.add(k, 1, 3);
		stage1.setOnHiding( event -> {System.out.println("Hiding Stage"); createPopupOpen = false;} );

		add.setOnAction( e-> { //show creation popup if not yet open
						if (!createPopupOpen) {
							createPopupOpen = true;
							newEntry.setVisible(true);
							stage1.show();
						}
				}
				);

		//		//workTable:
		//		gbc.gridx = 0; gbc.gridy = 1;
		//		workModel = new WorkTable();
		//		workTable = new JTable(workModel);
		//		workModel.loadTable();
		//		workTable.getTableHeader().setReorderingAllowed(false);
		//
		//		tasks.add(new JScrollPane(workTable), gbc);
		//		/*
		//		 * workTable.getColumnModel().getColumn(1).setPreferredWidth(30);
		//		 * workTable.getColumnModel().getColumn(2).setPreferredWidth(10);
		//		 * workTable.getColumnModel().getColumn(3).setPreferredWidth(10);
		//		 * workTable.getColumnModel().getColumn(4).setPreferredWidth(10);
		//		 */
		//
		//		reviewModel = new ReviewTable();
		//		reviewTable = new JTable(reviewModel);
		//		reviewModel.loadTable();
		//		reviewTable.getTableHeader().setReorderingAllowed(false);
		//		gbc.gridx = 1;
		//		tasks.add(new JScrollPane(reviewTable),gbc);
		//
		//		//sort upon click
		//		JTableHeader workHeader = workTable.getTableHeader();
		//		workHeader.addMouseListener(new WorkTableHeaderMouseListener());
		//		JTableHeader reviewHeader = reviewTable.getTableHeader();
		//		reviewHeader.addMouseListener(new ReviewTableHeaderMouseListener());
		//
		//		//workTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		//		System.out.println("\nNow Printing Work[]: ");
		//		printWork();
		show = new GridPane();
		home.getChildren().add(show);	
		home.setAlignment(Pos.TOP_CENTER); 
		showSelected(0); //default show today

		Scene scene = new Scene(tabPane, 1200, 600);
		primaryStage.setTitle("TimeBudget");
		primaryStage.setScene(scene);
		primaryStage.show();

	}

	public class WorkTableHeaderMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent event) {
			Point point = event.getPoint();
			int column = workTable.columnAtPoint(point);
			System.out.println("Header " + column + " clicked");
			//			workModel.loadTable(column); //workColNames[column]
			//repaint(); //refresh
		}
	}

	public class ReviewTableHeaderMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent event) {
			Point point = event.getPoint();
			int column = workTable.columnAtPoint(point);
			System.out.println("Header " + column + " clicked");
			//			reviewModel.loadTable(column); //workColNames[column]
			//repaint(); //refresh
		}
	}

	private class WorkTable extends DefaultTableModel {
		public void refreshAll() {
			fireTableDataChanged();
		}
		public void removeRows(int[] rows) 
		{
			for (int i = 0; i < rows.length; i++)
				removeRow(rows[i]);
		}
		public void loadTable() {
			loadTable(workTableSortedByCol);
		}
		public void loadTable(int sortByCol) {
			workTableSortedByCol = sortByCol;
			setRowCount(0);
			runSQL("select * from " + tableName + " order by " + workColNames[sortByCol] + ";", false);
			try {
				while(rs.next())
					workModel.addRow(new Object[]{rs.getInt("id"),rs.getString("name"), rs.getDate("deadline"), rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed")});
				//System.out.printf("%s:%s:%s\n", rs.getString("id"), rs.getString("name"), rs.getString("fixed"));
				System.out.println("successfully imported mySQL workTable to JTable");
			} catch (SQLException e) {e.printStackTrace(); System.err.println("load workTable failed");}
		}
		public Class<?> getColumnClass(int column) {
			switch (column) {
			case 0:	//id
				return Integer.class;
			case 1:	//name
				return String.class;
			case 2:	//deadline
				return String.class;
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
		public void setValueAt(Object value, int row, int col) {
			if (col == 2) {
				try {
					LocalDate d = LocalDate.parse((String) value);
					updateWork((int)workModel.getValueAt(row, 0), col, d); 
				} catch (Exception e) { System.err.println(value+" invalid date"); }
			}
			else
				updateWork((int)workModel.getValueAt(row, 0), col, value); //0th col of row is mysql id
		}
		public boolean isCellEditable(int row, int col) {
			return (col != 0); //id col not editable
		}
	}

	private class ReviewTable extends DefaultTableModel {
		public boolean isCellEditable(int row, int col) {
			return (col == 7 || col == 6); //only isDone and duration editable
		}
		public void setValueAt(Object value, int row, int col) {
			if (col == 7 || col == 6) updateReview((int)reviewModel.getValueAt(row, 0), col, value); 
		}

		public void removeRows(int[] rows) 
		{
			for (int i = 0; i < rows.length; i++) removeRow(rows[i]);
		}
		public void loadTable(int sortByCol) {
			reviewTableSortedByCol = sortByCol;
			runSQL("select * from " + reviewTableName + " order by " + reviewColNames[sortByCol] + ";", false);
			setRowCount(0);
			try {
				while(rs.next())
					reviewModel.addRow(new Object[]{rs.getInt("id"),rs.getInt("classID"), classNames.get(rs.getInt("classID")), rs.getDouble("lectID"), rs.getDate("lecture"), rs.getDate("deadline"), rs.getDouble("hr"), rs.getBoolean("isDone")});
				//System.out.printf("%s:%s:%s\n", rs.getString("id"), rs.getString("name"), rs.getString("fixed"));
				System.out.println("successfully imported mySQL review workTable to JTable");
			} catch (SQLException e) {e.printStackTrace(); System.err.println("load review workTable failed");}
		}
		public void loadTable() {
			loadTable(reviewTableSortedByCol);
		}

		public Class<?> getColumnClass(int column) {
			switch (column) {
			case 0:	//id
			case 1:	//classID
				return Integer.class;
			case 2: //className
				return String.class;
			case 3:	//lectID
				return Double.class;
			case 4:	//lecture
				return LocalDate.class;
			case 5:	//deadline
				return LocalDate.class;
			case 6:	//hr
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
			case 2: return "className";
			case 3: return "lectID";
			case 4: return "lecture";
			case 5: return "deadline";
			case 6: return "hr";
			default: return "isDone";
			}
		}
		public int getColumnCount() { return 8; }
	}

	private void removeWork(int id) {
		runSQL("delete from " + tableName + " where id = " + id, true);
		return;
	}

	private void removeReview(int id) {
		runSQL("update " + reviewTableName + " set isDone = 1 where id = " + id, true);
		return;
	}

	private void updateReview(int id, int col, Object value) {
		if (col == 7)
			runSQL("update " + reviewTableName + " set isDone = " + value + " where id = " + id, true);
		else
			runSQL("update " + reviewTableName + " set hr = " + value + " where id = " + id, true);
		editTasks=true;
	}
	private void updateWork(int id, int col, Object value) {
		switch(col) {
		case 1: 
			runSQL("update " + tableName + " set name = \"" + value + "\" where id = " + id, true);
			break;
		case 2: 
			runSQL("update " + tableName + " set deadline = \"" + value + "\" where id = " + id, true);
			break;
		case 3: 
			runSQL("update " + tableName + " set hr = " + value + " where id = " + id, true);
			break;
		case 4: 
			runSQL("update " + tableName + " set diff = " + value + " where id = " + id, true);
			break;
		case 5:
			runSQL("update " + tableName + " set fixed = " + value + " where id = " + id, true);
			break;
		}	
		editTasks=true;
		return;
	}

	void showSelected(int index) { 
		System.out.println("showSelected("+index+");");
		//shows both leetcode + play and assigned stuff
		msg.setText(String.valueOf(hrsOnHwToday));
		show.getChildren().clear();

		//		GridBagConstraints c = new GridBagConstraints();
		//		show.setLayout(new GridBagLayout());




		class checkWorkHandler implements EventHandler<ActionEvent> {
			public void handle(ActionEvent event) {
				for (int i = 0; i < workChecks.size(); ) {
					if (workChecks.get(i).getKey().isSelected()) {
						int id = workChecks.get(i).getValue(); //mysql id of the checked box
						double durDone = assignWork.get(index).get(i).getValue();
						hrsOnHwToday += durDone; //+= assigned duration for that entry = assignWork[day][i][1]
						msg.setText(String.valueOf(hrsOnHwToday));

						runSQL("select * from " + tableName + " where id = " + id, false);
						try {
							if (rs.next()) {
								double durLeft = rs.getDouble("hr");
								if (durLeft - durDone <= 0) removeWork(id);
								else updateWork(id, 2, durLeft - durDone); //col 3 is hr
							}
						} catch (SQLException e) {e.printStackTrace();}

						System.err.println("trying to remove "  + i);
						assignWork.get(drop.getSelectionModel().getSelectedIndex()).remove(i); //unassign checked item
						show.getChildren().remove(workChecks.get(i).getKey());
						workChecks.remove(i);
						//revalidate(); //to refresh removal
					}
					else i++;
				}
				//				workModel.loadTable();
			}
		}

		class checkReviewHandler implements EventHandler<ActionEvent> {
			public void handle(ActionEvent event) {
				for (int i = 0; i < reviewChecks.size(); ) {
					if (reviewChecks.get(i).getKey().isSelected()) {
						int id = reviewChecks.get(i).getValue(); //mysql id of the checked box
						double durDone = assignReview.get(index).get(i).getValue();
						hrsOnHwToday += durDone; //+= assigned duration for that entry = assignWork[day][i][1]
						msg.setText(String.valueOf(hrsOnHwToday));

						runSQL("select * from " + reviewTableName + " where id = " + id, false);
						try {
							if (rs.next()) {
								double durLeft = rs.getDouble("hr");
								if (durLeft - durDone <= 0) removeReview(id);
								else updateReview(id, 2, durLeft - durDone); //col 3 is hr
							}
						} catch (SQLException e) {e.printStackTrace();}

						System.err.println("trying to remove "  + i);
						assignReview.get(drop.getSelectionModel().getSelectedIndex()).remove(i); //unassign checked item
						show.getChildren().remove(reviewChecks.get(i).getKey());
						reviewChecks.remove(i);
						//revalidate(); //to refresh removal
					}
					else i++;
				}
				//				reviewModel.loadTable();
			}
		}

		//c.fill = GridBagConstraints.HORIZONTAL;    //fill entire cell with text to center
		//c.gridwidth = 4; c.gridx = 0; c.gridy = 0;   //coords + width of msg element
		int gridx = 0; int gridy= 0;
		workChecks = new ArrayList<SimpleEntry<CheckBox, Integer>>(); //handler needs to check if is ReviewEntry, so also store int (index to assignWork)
		reviewChecks = new ArrayList<SimpleEntry<CheckBox, Integer>>(); //handler needs to check if is ReviewEntry, so also store int (index to assignWork)
		checkWorkHandler workHandler = new checkWorkHandler();
		checkReviewHandler reviewHandler = new checkReviewHandler();
		for (int i = 0; i < assignWork.get(index).size(); i++) {
			runSQL("select * from " + tableName + " where id = " + assignWork.get(index).get(i).getKey() + ";", false);
			Entry temp = null;
			try {
				if (rs.next()) {
					LocalDate dueDate = rs.getDate("deadline").toLocalDate();
					temp = new Entry(rs.getInt("id"), rs.getString("name"), dueDate, rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed"));
					CheckBox b = new CheckBox(temp.name + ",  " + temp.hr + "h");
					SimpleEntry<CheckBox, Integer> a = new SimpleEntry<CheckBox, Integer>(b, assignWork.get(index).get(i).getKey());
					workChecks.add(a);
					workChecks.get(workChecks.size()-1).getKey().setTooltip(new Tooltip("due " + formatter.format(temp.deadline)));
					//System.out.println(":(" + workChecks.get(workChecks.size()-1).getKey().getText() + " " + workChecks.get(workChecks.size()-1).getValue());

					workChecks.get(workChecks.size()-1).getKey().setOnAction(workHandler);
					show.add(workChecks.get(workChecks.size()-1).getKey(), gridx, gridy);
					gridy++;
					//					workModel.loadTable();
				}
			} catch (SQLException e) {e.printStackTrace();}
		}
		for (int i = 0; i < assignReview.get(index).size(); i++) {
			runSQL("select * from " + reviewTableName + " where id = " + assignReview.get(index).get(i).getKey() + ";", false);
			Entry temp = null;
			try {
				if (rs.next()) {
					LocalDate dueDate = rs.getDate("deadline").toLocalDate();
					temp = new Entry(rs.getInt("id"), classNames.get(rs.getInt("classID")) + " Lect " + rs.getString("lectID"), dueDate, rs.getDouble("hr"), 1, false);
					CheckBox b = new CheckBox(temp.name + ",  " + temp.hr + "h");
					SimpleEntry<CheckBox, Integer> a = new SimpleEntry<CheckBox, Integer>(b, assignReview.get(index).get(i).getKey());
					reviewChecks.add(a);
					reviewChecks.get(reviewChecks.size()-1).getKey().setTooltip(new Tooltip("due " + formatter.format(temp.deadline)));
					//System.out.println(":(" + workChecks.get(workChecks.size()-1).getKey().getText() + " " + workChecks.get(workChecks.size()-1).getValue());

					reviewChecks.get(reviewChecks.size()-1).getKey().setOnAction(reviewHandler);
					show.add(reviewChecks.get(reviewChecks.size()-1).getKey(), gridx, gridy);
					gridy++;
					//					reviewModel.loadTable();
				}
			} catch (SQLException e) {e.printStackTrace();}
		}
		stats = new Label(hrsLeft.get(index) + "/" + (onBreak? breakBudget.get(index) : budget.get(index)) + "h free"); 
		show.add(stats,gridx,gridy);
		if (hrsLeft.get(index) > 0)
		{
			gridy++; //%.2f to format double show 2 decimal places max
			leet = new Label(formatDuration(hrsLeft.get(index) * 0.6) + " for CS");
			show.add(leet, gridx, gridy);
			gridy++;
			play = new Label(formatDuration(hrsLeft.get(index) * 0.4) + " to play");
			show.add(play, gridx, gridy);
		}	

		//revalidate();
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
				System.out.print(formatter.format(week.get(i)) + ":" + week.get(i).getDayOfWeek() + " and # assigned: " + assignWork.get(i).size() + "\n\t" );
				for (int j = 0; j < assignWork.get(i).size(); j++) {
					System.out.println(assignWork.get(i).get(j).getKey() + " of len " + assignWork.get(i).get(j).getValue());
					runSQL("select * from " + tableName + " where id = " + assignWork.get(i).get(j).getKey() + ";", false);

					try {
						if (rs.next())
							System.out.printf("%s\t%s\t%s\t%s/%s\t%s\t%s\n",rs.getInt("id"),rs.getString("name"), rs.getDate("deadline"), assignWork.get(i).get(j).getValue(),rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed"));
					} catch (SQLException e) {e.printStackTrace();}
				}
				System.out.print("\n\t" + hrsLeft.get(i) + "h out of " + breakBudget.get(i) + "h free\n\n");
			}
		}
		else {
			for (int i = 0; i < 7; i++)
			{
				System.out.print(formatter.format(week.get(i)) + ":" + week.get(i).getDayOfWeek() + " and # assigned: " + assignWork.get(i).size() + "\n\t" );
				for (int j = 0; j < assignWork.get(i).size(); j++) {
					System.out.println(assignWork.get(i).get(j).getKey() + " of len " + assignWork.get(i).get(j).getValue());
					runSQL("select * from " + tableName + " where id = " + assignWork.get(i).get(j).getKey() + ";", false);

					try {
						if (rs.next())
							System.out.printf("%s\t%s\t%s\t%s/%s\t%s\t%s\n",rs.getInt("id"),rs.getString("name"), rs.getDate("deadline"), assignWork.get(i).get(j).getValue(),rs.getDouble("hr"), rs.getInt("diff"), rs.getBoolean("fixed"));
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
		assignWork = new ArrayList<List<SimpleEntry<Integer, Double>>>(); //reset assignWork
		assignReview = new ArrayList<List<SimpleEntry<Integer, Double>>>(); //reset assignWork

		for (int i = 0; i < 7; i++)
		{
			ArrayList<SimpleEntry<Integer,Double>> temp = new ArrayList<SimpleEntry<Integer,Double>>();
			ArrayList<SimpleEntry<Integer,Double>> temp2 = new ArrayList<SimpleEntry<Integer,Double>>();
			assignWork.add(temp);
			assignReview.add(temp2);
		}

		runSQL("select * from " + tableName + " order by fixed desc, deadline, diff desc", false);

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

		runSQL("select * from " + reviewTableName + " order by deadline", false);

		try {
			while (rs.next() && (LocalDate.parse(rs.getString("lecture")).isBefore(today) || LocalDate.parse(rs.getString("lecture")).isEqual(today))) {
				if (rs.getBoolean("isDone") == true) continue;
				LocalDate dueDate = rs.getDate("deadline").toLocalDate();
				Entry temp = new Entry(rs.getInt("id"), rs.getString("classID") + " " + rs.getString("lectID"), LocalDate.parse(rs.getString("deadline")), rs.getDouble("hr"), 1, false);




				assignEntry(temp, true);
			}
		} catch (SQLException e) {e.printStackTrace();}

		printAssigned();
	}

	private void assignEntry(Entry curr, boolean isReview) { 
		List<SimpleEntry<Integer, Integer>> report = new LinkedList<SimpleEntry<Integer, Integer>>(); //list of all assign coords 
		int n = (int) ChronoUnit.DAYS.between(today, curr.deadline);
		System.out.printf("assignEntry(" + curr.id + ", %s);\n", (isReview? "t" : "s"));
		if (!(n >= 0 && n < 7)) {
			System.err.println("\tsorry, n not right range:" + n);
			return;
		}
		if (n<=0 && isReview) n = 8; //deadline passed, assignWork review to any day this week

		if (curr.isFixed) 
		{
			assignWork.get((int) n).add(new SimpleEntry<Integer, Double>(curr.id, curr.hr)); //id of mysql entry assigned to date n's list
			report.add(new SimpleEntry<Integer,Integer>(n, assignWork.size()-1));
			hrsLeft.set((int) n, hrsLeft.get((int) n) - curr.hr);
		}
		else {
			if (n==0) n++;
			int idealN = -1;

			for (int j = 0; j < n-1; j++) //assignWork today? tmrw? day after? 
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
			{ //forced to assignWork assignWork the day of deadline

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
						for (int j = 0; j < n-1 && doCont; j++) //assignWork today? tmrw? day after? 
						{
							System.out.println("trying day " + j);
							double extraTime = hrsLeft.get(j) - Ideal(week.get(j));
							if (extraTime > 0) //has some extra time even w/ ideal leet+play
							{
								if (remainingUnassigned <= extraTime) { //doneReviews! all parts fitted
									doCont = false;
									if (isReview)
										assignReview.get(j).add(new SimpleEntry<Integer, Double> (curr.id, remainingUnassigned)); //direct assignWork to day j
									else
										assignWork.get(j).add(new SimpleEntry<Integer, Double> (curr.id, remainingUnassigned)); //direct assignWork to day j
									report.add(new SimpleEntry<Integer,Integer>(j, (isReview)? assignReview.size()-1 : assignWork.size()-1));
									hrsLeft.set(j, hrsLeft.get(j) - remainingUnassigned); //update hours of day j
								}
								else { //gotta do more splitting, take entire extraTime
									if (isReview)
										assignReview.get(j).add(new SimpleEntry<Integer, Double>(curr.id, extraTime));
									else
										assignWork.get(j).add(new SimpleEntry<Integer, Double>(curr.id, extraTime));
									report.add(new SimpleEntry<Integer,Integer>(j, (isReview)? assignReview.size()-1 : assignWork.size()-1));
									remainingUnassigned -= extraTime;
									hrsLeft.set(j, (double) Ideal(week.get(j))); //which eats up all of day j's bonus hrs left.
								}
							}
						}
						if (doCont) //still need to assignWork remaining portion, last resort is to deadline
						{
							if (isReview)
								assignReview.get((int) n-1).add(new SimpleEntry<Integer, Double>(curr.id, remainingUnassigned)); //index of remaining portion assigned to date n's list
							else
								assignWork.get((int) n-1).add(new SimpleEntry<Integer, Double>(curr.id, remainingUnassigned)); //index of remaining portion assigned to date n's list
							report.add(new SimpleEntry<Integer,Integer>(n-1, (isReview)? assignReview.size()-1 : assignWork.size()-1));
							hrsLeft.set((int) n-1, hrsLeft.get((int) n-1) - remainingUnassigned); //subtract remain portion time from deadline hrsleft
						}
					}
					else { //old alg:
						System.out.println("unfortunate split case");
						double takenLPTime = 0; //LP = leet + play time
						double needLPTTake = curr.hr - sumAsIdeal; //how much leet+play time must be taken total from day 0 to day b4 deadline
						for (int j = 0; j < n-1 && doCont; j++) //assignWork today? tmrw? day after? 
						{
							if (takenLPTime < needLPTTake) { //need to take all of today's time. 
								if (hrsLeft.get(j) > 0) //has some time to squeeze this task
								{
									if (remainingUnassigned <= hrsLeft.get(j)) { //all parts fitted
										doCont = false;
										if(isReview)
											assignReview.get(j).add(new SimpleEntry<Integer, Double>(curr.id, remainingUnassigned)); //direct assignWork to day j
										else
											assignWork.get(j).add(new SimpleEntry<Integer, Double>(curr.id, remainingUnassigned)); //direct assignWork to day j
										report.add(new SimpleEntry<Integer,Integer>(j, (isReview)? assignReview.size()-1 : assignWork.size()-1));
										hrsLeft.set(j, hrsLeft.get(j) - remainingUnassigned); //update hours of day j
									}
									else { //gotta do more splitting, take entire day's time
										if(isReview)
											assignReview.get(j).add(new SimpleEntry<Integer, Double>(curr.id, hrsLeft.get(j)));
										else
											assignWork.get(j).add(new SimpleEntry<Integer, Double>(curr.id, hrsLeft.get(j)));
										report.add(new SimpleEntry<Integer,Integer>(j, (isReview)? assignReview.size()-1 : assignWork.size()-1));
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
										if(isReview)
											assignReview.get(j).add(new SimpleEntry<Integer,Double>(curr.id, remainingUnassigned)); //direct assignWork to day j
										else
											assignWork.get(j).add(new SimpleEntry<Integer,Double>(curr.id, remainingUnassigned)); //direct assignWork to day j
										report.add(new SimpleEntry<Integer,Integer>(j, (isReview)? assignReview.size()-1 : assignWork.size()-1));
										hrsLeft.set(j, extraTime - remainingUnassigned); //update hours of day j
									}
									else { //gotta do more splitting. Greedy, take entire bonus time of day j
										if(isReview)
											assignReview.get(j).add(new SimpleEntry<Integer,Double>(curr.id, extraTime));
										else
											assignWork.get(j).add(new SimpleEntry<Integer,Double>(curr.id, extraTime));
										report.add(new SimpleEntry<Integer,Integer>(j, (isReview)? assignReview.size()-1 : assignWork.size()-1));
										remainingUnassigned -= extraTime; //update remaining portion of task
										hrsLeft.set(j, (double) Ideal(week.get(j))); //which eats up all of day j's hrs left.
									}
								}
							}
						}
						if (doCont) //still need to assignWork remaining portion, last resort is to deadline
						{
							if (isReview)
								assignReview.get((int) n-1).add(new SimpleEntry<Integer, Double>(curr.id, remainingUnassigned)); //index of remaining portion assigned to date n's list
							else
								assignWork.get((int) n-1).add(new SimpleEntry<Integer, Double>(curr.id, remainingUnassigned)); //index of remaining portion assigned to date n's list
							report.add(new SimpleEntry<Integer,Integer>(n-1, (isReview)? assignReview.size()-1 : assignWork.size()-1));
							hrsLeft.set((int) n-1, hrsLeft.get((int) n-1) - remainingUnassigned); //subtract remain portion time from deadline hrsleft
						}
					}
				}
				else { //deadline only day enough time for entire assignment, so assignWork to deadline
					if (isReview)
						assignReview.get((int) n-1).add(new SimpleEntry<Integer, Double>(curr.id, curr.hr)); //index of remaining portion assigned to date n's list
					else
						assignWork.get((int) n-1).add(new SimpleEntry<Integer, Double>(curr.id, curr.hr)); //index of remaining portion assigned to date n's list
					report.add(new SimpleEntry<Integer,Integer>(n-1, (isReview)? assignReview.size()-1 : assignWork.size()-1));
					hrsLeft.set((int) n-1, hrsLeft.get((int) n-1) - curr.hr); //subtract remain portion time from deadline hrsleft
				}
			}
			else //ideal fits perfectly, just assignWork it to ideal day.
			{
				if (isReview)
					assignReview.get(idealN).add(new SimpleEntry<Integer, Double>(curr.id, curr.hr)); //index of work assigned to date idealN's list
				else
					assignWork.get(idealN).add(new SimpleEntry<Integer, Double>(curr.id, curr.hr)); //index of work assigned to date idealN's list
				report.add(new SimpleEntry<Integer,Integer>(idealN, (isReview)? assignReview.size()-1 : assignWork.size()-1));
				hrsLeft.set(idealN, hrsLeft.get(idealN) - curr.hr);
			}
		}

		if (isReview) {System.out.println("review!");}
		System.err.println("report.size = " + report.size());
		for (int i = 0; i < report.size(); i++) {
			System.err.println("day "+ report.get(i).getKey());
			if (isReview) {

				/*if (curr.id != assignReview.get(report.get(i).getKey()).get(report.get(i).getValue()).getKey())  {
					System.out.println("WARNING");
					System.exit(0);
				}*/
			}
			else {
				/*if (curr.id != assignWork.get(report.get(i).getKey()).get(report.get(i).getValue()).getKey())  {
					System.out.println("WARNING");
					System.exit(0);
				}*/
			}
			//System.err.println(report.get(i).getKey() + ":" + report.get(i).getValue());
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


		Scanner getX = null;
		try {
			getX = new Scanner(reviewClassesFile);
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }

		if (getX != null)
		{
			if (!doGenerate) {
				while(getX.hasNextLine())
				{
					String line = getX.nextLine();
					String name = line.substring(1, line.indexOf("\"",1));
					classNames.add(name);
				}
				return;
			}

			int classIndex = 0;
			while(getX.hasNextLine())
			{
				String line = getX.nextLine();
				String name = line.substring(1, line.indexOf("\"",1));
				line = line.substring(line.indexOf("\"", 1)+2); //remove name portion
				String temp1[] = line.split(",");
				classNames.add(name);

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
				if (loadTable) {
					//					workModel.loadTable();
					//					reviewModel.loadTable();
					editTasks = true; //mark for rerun scheduler once switch to home tab
				}
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
