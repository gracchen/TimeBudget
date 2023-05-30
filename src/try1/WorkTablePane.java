package try1;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;

import javafx.beans.property.BooleanProperty;
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

public class WorkTablePane {
	private GUI g; //way to talk back to main GUI class
	private Connect c;
	private WorkTable workTable;
	private String tableName;
	private TableView<Entry> tableView;
	private ScrollPane pane;
	
	public WorkTablePane(String name, GUI g) {
		this.g = g;
		this.c = g.c;
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
			g.runSQL("update " + tableName + " set name = \"" + value + "\" where id = " + id, true);
			break;
		case 2: 
			g.runSQL("update " + tableName + " set deadline = \"" + value + "\" where id = " + id, true);
			break;
		case 3: 
			g.runSQL("update " + tableName + " set hr = " + value + " where id = " + id, true);
			break;
		case 4: 
			g.runSQL("update " + tableName + " set diff = " + value + " where id = " + id, true);
			break;
		case 5:
			g.runSQL("update " + tableName + " set fixed = " + value + " where id = " + id, true);
			break;
		}	
		return;
	}

	
	public void removeWork(int id) {
		g.runSQL("delete from " + tableName + " where id = " + id, true);
		return;
	}

	public void removeSelected() {
		workTable.removeRows(tableView.getSelectionModel().getSelectedIndices());
	}
	
	void printWork() {
		workTable.loadTable();
	}
	
	private class WorkTable {
		public ObservableList<Entry> data;
		
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
	        fixedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(fixedColumn));
	        //fixedColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().getFixed()));

	        fixedColumn.setCellValueFactory(cellData -> cellData.getValue().getFixedProperty());
	        //fixedColumn.setCellValueFactory(new PropertyValueFactory<Entry, Boolean>("fixed"));
/*
 * 
	        fixedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(new Callback<Integer, ObservableValue<Boolean>>() {
				@Override
				public ObservableValue<Boolean> call(Integer arg0) {
			        System.out.println("Cours "+ arg0 + " changed value.");
			        return data.get(arg0).getFixedProperty();
				}
	        }));
 */
	        fixedColumn.setEditable(true);
	        //fixedColumn.setOnEditCommit(e->e.getTableView().getItems().get(e.getTablePosition().getRow()).setFixed(e.getNewValue()));
	        /*
	        fixedColumn.setOnEditCommit(e -> {
	        Entry entry = e.getRowValue();
	        entry.setFixed();
	        System.out.println("EHE");
	        });*/
	        
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
			g.runSQL("delete from " + tableName + " where id = " + data.get(row).getId(), true);
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
			g.runSQL("select * from " + tableName + ";", false);
			try {
				while(c.rs.next())
					data.add(new Entry(c.rs.getInt("id"),c.rs.getString("name"), c.rs.getDate("deadline").toLocalDate(), c.rs.getDouble("hr"), c.rs.getInt("diff"), c.rs.getBoolean("fixed")));
				//System.out.printf("%s:%s:%s\n", c.rs.getString("id"), c.rs.getString("name"), c.rs.getString("fixed"));
				System.out.println("successfully imported mySQL workTable to JTable");
			} catch (SQLException e) {e.printStackTrace(); System.err.println("load workTable failed");}
		}
	}


	public class Entry {
		private int id;
		private String name;
		private LocalDate deadline;
		private double hr;
		private int diff;
		private BooleanProperty fixed;

		Entry(int i, String n, LocalDate de, double h, int di, boolean t) {
			id = i;
			name = n;
			deadline = de;
			hr = h;
			diff = di;
			fixed = new SimpleBooleanProperty(t);
			fixed.addListener((observable, oldValue, newValue) -> {
				if (oldValue != newValue)
				{
					System.out.println(">" + oldValue + "-->" + newValue);
					fixed.set(newValue); //flip value locally
					g.runSQL("update " + tableName + " set fixed = " + newValue + " where id = " + id, true); //and remotely
				}
			});
		}

		public int getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
			g.runSQL("update " + tableName + " set name = \"" + name + "\" where id = " + id, true);
		}
		
		public LocalDate getDeadline() {
			return deadline;
		}
		public void setDeadline(LocalDate string) {
			this.deadline = string;
			g.runSQL("update " + tableName + " set deadline = \"" + string + "\" where id = " + id, true);
		}
		
		public double getHr() {
			return hr;
		}
		public void setHr(Double newValue) {
			hr = newValue;
			g.runSQL("update " + tableName + " set hr = " + newValue + " where id = " + id, true);
		}
		
		public int getDiff() {
			return diff;
		}
		public void setDiff(int newValue) {
			diff = newValue;
			g.runSQL("update " + tableName + " set diff = " + newValue + " where id = " + id, true);

		}
		
		public boolean getFixed() {
			return fixed.get();
		}   

		public BooleanProperty getFixedProperty() {
			return fixed;
		}
	}
	
	
}

