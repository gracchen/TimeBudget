package try1;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LocalDateStringConverter;
import try1.GUI.Entry;

public class WorkTablePane {
	private Connect c;
	private WorkTable workTable;
	private String tableName;
	private TableView<Entry> tableView;
	private ScrollPane pane;
	
	public WorkTablePane(String name) {
		c = new Connect();
		this.tableName = name;
		workTable = new WorkTable();
		tableView = workTable.createTableView();
		workTable.loadTable();
		pane = new ScrollPane(tableView);
		pane.setFitToWidth(true);
		pane.setFitToHeight(true);
	}
	
	public ScrollPane getScrollPane() {
		return pane;
	}
	public void updateWork(int id, int col, Object value) {
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
		return;
	}

	
	public void removeWork(int id) {
		runSQL("delete from " + tableName + " where id = " + id, true);
		return;
	}

	public void removeSelected() {
		workTable.removeRows(tableView.getSelectionModel().getSelectedIndices());
	}
	
	void printWork() {
		workTable.loadTable();
	}
	
	private class WorkTable {
		private ObservableList<Entry> data;
		
		public TableView<Entry> createTableView() {
			data = FXCollections.observableArrayList();
			TableView<Entry> tableView = new TableView<Entry>(data);

	        TableColumn<Entry, Integer> idColumn = new TableColumn<>("ID");
	        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
	        idColumn.setReorderable(false);
	        
	        TableColumn<Entry, String> nameColumn = new TableColumn<>("Name");
	        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
	        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
	        nameColumn.setOnEditCommit(e->e.getTableView().getItems().get(e.getTablePosition().getRow()).setName(e.getNewValue()));
	        nameColumn.setReorderable(false);

	        TableColumn<Entry, LocalDate> deadlineColumn = new TableColumn<>("Deadline");
	        deadlineColumn.setCellFactory(TextFieldTableCell.forTableColumn(new LocalDateStringConverter()));
	        deadlineColumn.setCellValueFactory(new PropertyValueFactory<>("deadline"));
	        deadlineColumn.setReorderable(false);
	        deadlineColumn.setOnEditCommit(e->
	        {
	        	try {
	        		e.getTableView().getItems().get(e.getTablePosition().getRow()).setDeadline(e.getNewValue());
	        	} catch (Exception e1) {System.err.println("oops");}
	        });

	        TableColumn<Entry, Double> hrColumn = new TableColumn<>("Hr");
	        hrColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
	        hrColumn.setCellValueFactory(new PropertyValueFactory<>("hr"));
	        hrColumn.setOnEditCommit(e->e.getTableView().getItems().get(e.getTablePosition().getRow()).setHr(e.getNewValue()));
	        hrColumn.setReorderable(false);

	        TableColumn<Entry, Integer> diffColumn = new TableColumn<>("Diff");
	        diffColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
	        diffColumn.setCellValueFactory(new PropertyValueFactory<>("diff"));
	        diffColumn.setOnEditCommit(e->e.getTableView().getItems().get(e.getTablePosition().getRow()).setDiff(e.getNewValue()));
	        diffColumn.setReorderable(false);

	        TableColumn<Entry, Boolean> fixedColumn = new TableColumn<>("Fixed");
	     // Set the cell factory for the fixedColumn to use CheckBoxTableCell
	        fixedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(fixedColumn));
	        
	        // Map the fixedColumn to the corresponding property in the Entry class
	        fixedColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().getFixed()));
	        fixedColumn.setOnEditCommit(e -> {
	            Entry entry = e.getRowValue();
	            entry.setFixed();
	            System.out.println("EHE");
	        });
	        
	        fixedColumn.setReorderable(false);

	        tableView.getColumns().addAll(idColumn, nameColumn, deadlineColumn, hrColumn, diffColumn, fixedColumn);
	        tableView.setEditable(true); 
	        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	        
	        return tableView;
		}

		public void addRow(Entry e) {
			data.add(e);
		}

		public void removeRow(int row) {
			runSQL("delete from " + tableName + " where id = " + data.get(row).getId(), true);
			System.out.println("removing id " + data.get(row).getId() + " of name " + data.get(row).getName());
			data.remove(row);
		}
		
		public void removeRows(ObservableList<Integer> observableList) 
		{
			ArrayList<Integer> selectedList = new ArrayList<>(observableList);
			Collections.sort(selectedList, Collections.reverseOrder());
			
			for (int i = 0; i < selectedList.size(); i++)
				removeRow(selectedList.get(i));
		}

		public void loadTable() {
			data.clear();
			//loadTable(workTableSortedByCol);
			runSQL("select * from " + tableName + ";", false);
			try {
				while(c.rs.next())
					data.add(new Entry(c.rs.getInt("id"),c.rs.getString("name"), c.rs.getDate("deadline").toLocalDate(), c.rs.getDouble("hr"), c.rs.getInt("diff"), c.rs.getBoolean("fixed")));
				//System.out.printf("%s:%s:%s\n", c.rs.getString("id"), c.rs.getString("name"), c.rs.getString("fixed"));
				System.out.println("successfully imported mySQL workTable to JTable");
			} catch (SQLException e) {e.printStackTrace(); System.err.println("load workTable failed");}
		}
	}


	int runSQL(String query, boolean loadTable) {
		if (query.indexOf("select") == 0) {
			try {
				c.rs = c.st.executeQuery(query);
				System.out.println(query + " was successful");
				return 0;
			} catch (SQLException e) {e.printStackTrace(); System.err.println(query + " failed"); return -1;}
		}
		else {
			try {
				c.st.executeUpdate(query);
				System.out.println(query + " was successful");
				if (loadTable) {
					//					workModel.loadTable();
					//					reviewModel.loadTable();
					//!!!editTasks = true; //mark for rerun scheduler once switch to home tab
				}
				return 0;
			} catch (SQLException e) {e.printStackTrace(); System.err.println(query + " failed"); return -1;}
		}


	}

	public class Entry {
		private int id;
		private String name;
		private LocalDate deadline;
		private double hr;
		private int diff;
		private boolean fixed;

		Entry(int i, String n, LocalDate de, double h, int di, boolean t) {
			id = i;
			name = n;
			deadline = de;
			hr = h;
			diff = di;
			fixed = t;
		}

		public int getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
			runSQL("update " + tableName + " set name = \"" + name + "\" where id = " + id, true);
		}
		
		public LocalDate getDeadline() {
			return deadline;
		}
		public void setDeadline(LocalDate string) {
			this.deadline = string;
			runSQL("update " + tableName + " set deadline = \"" + string + "\" where id = " + id, true);
		}
		
		public double getHr() {
			return hr;
		}
		public void setHr(Double newValue) {
			hr = newValue;
			runSQL("update " + tableName + " set hr = " + newValue + " where id = " + id, true);
		}
		
		public int getDiff() {
			return diff;
		}
		public void setDiff(int newValue) {
			diff = newValue;
			runSQL("update " + tableName + " set diff = " + newValue + " where id = " + id, true);

		}
		
		public boolean getFixed() {
			return fixed;
		}
		public void setFixed() {
			fixed = !fixed;
			System.out.println("fixed is now = " + fixed);
			runSQL("update " + tableName + " set fixed = " + fixed + " where id = " + id, true);
		}
		
	}
	
	
}

