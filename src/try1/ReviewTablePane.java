package try1;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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

public class ReviewTablePane {
	private GUI g;
	private Connect c;
	private ReviewTable reviewTable;
	private String tableName;
	private TableView<ReviewEntry> reviewTableView;
	private ScrollPane pane;
	private List<String> classNames = new LinkedList<String>();

	public ReviewTablePane(String name, List<String> names, GUI g) {
		this.g = g;
		this.c = g.c;
		classNames = names;
		tableName = name;
		reviewTable = new ReviewTable();
		reviewTableView = reviewTable.createTableView();
		reviewTable.loadTable();
		pane = new ScrollPane(reviewTableView);
		pane.setFitToWidth(true);
		pane.setFitToHeight(true);
	}
	public void removeReview(int id) {
		g.runSQL("update " + tableName + " set isDone = 1 where id = " + id, true);
		return;
	}

	public void updateReview(int id, int col, Object value) {
		if (col == 7)
			g.runSQL("update " + tableName + " set isDone = " + value + " where id = " + id, true);
		else
			g.runSQL("update " + tableName + " set hr = " + value + " where id = " + id, true);
	}
	
	public ScrollPane getScrollPane() {
		return pane;
	}

	
	private class ReviewTable {
		private ObservableList<ReviewEntry> data;
		
		public TableView<ReviewEntry> createTableView() {
			data = FXCollections.observableArrayList();
			TableView<ReviewEntry> tableView = new TableView<ReviewEntry>(data);

	        TableColumn<ReviewEntry, Integer> idColumn = new TableColumn<>("ID");
	        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
	        idColumn.setReorderable(false);
	        
	        TableColumn<ReviewEntry, Integer> classIDColumn = new TableColumn<>("classID");
	        classIDColumn.setCellValueFactory(new PropertyValueFactory<>("classID"));
	        classIDColumn.setReorderable(false);

	        TableColumn<ReviewEntry, String> classColumn = new TableColumn<>("Class");
	        classColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
	        classColumn.setReorderable(false);
	        
	        TableColumn<ReviewEntry, Double> lectIDColumn = new TableColumn<>("lectID");
	        lectIDColumn.setCellValueFactory(new PropertyValueFactory<>("lectID"));
	        lectIDColumn.setReorderable(false);

	        TableColumn<ReviewEntry, LocalDate> lectureColumn = new TableColumn<>("Lecture");
	        lectureColumn.setCellValueFactory(new PropertyValueFactory<>("lecture"));
	        lectureColumn.setReorderable(false);

	        TableColumn<ReviewEntry, LocalDate> deadlineColumn = new TableColumn<>("Deadline");
	        deadlineColumn.setCellValueFactory(new PropertyValueFactory<>("deadline"));
	        deadlineColumn.setReorderable(false);

	        TableColumn<ReviewEntry, Double> hrColumn = new TableColumn<>("Hr");
	        hrColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
	        hrColumn.setCellValueFactory(new PropertyValueFactory<>("hr"));
	        hrColumn.setOnEditCommit(e->e.getTableView().getItems().get(e.getTablePosition().getRow()).setHr(e.getNewValue()));
	        hrColumn.setReorderable(false);

	        TableColumn<ReviewEntry, Boolean> doneColumn = new TableColumn<>("IsDone");
	        doneColumn.setCellFactory(CheckBoxTableCell.forTableColumn(doneColumn));
	        doneColumn.setCellValueFactory(cellData -> cellData.getValue().getIsDoneProperty());
	        
	        doneColumn.setReorderable(false);

	        tableView.getColumns().addAll(idColumn, classIDColumn, classColumn, lectIDColumn,lectureColumn, deadlineColumn, hrColumn, doneColumn);
	        tableView.setEditable(true); 
	        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	        
	        return tableView;
		}

		public void addRow(ReviewEntry e) {
			data.add(e);
		}

		
		public void removeRow(int row) {
			g.runSQL("delete from " + tableName + " where id = " + data.get(row).getId(), true);
			System.out.println("removing id " + data.get(row).getId() + " of name " + data.get(row).getClass());
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
					data.add(new ReviewEntry(c.rs.getInt("id"),c.rs.getInt("classID"),classNames.get(c.rs.getInt("classID")), c.rs.getDouble("lectID"), c.rs.getDate("lecture").toLocalDate(), c.rs.getDate("deadline").toLocalDate(), c.rs.getDouble("hr"), c.rs.getBoolean("isDone")));
				
				//System.out.printf("%s:%s:%s\n", c.rs.getString("id"), c.rs.getString("name"), c.rs.getString("fixed"));
				System.out.println("successfully imported mySQL workTable to JTable");
			} catch (SQLException e) {e.printStackTrace(); System.err.println("load workTable failed");}
		}
	}

	public class ReviewEntry {
		private int id;
		private int classID;
		private String className;
		private Double lectID;
		private LocalDate lecture;
		private LocalDate deadline;
		private double hr;
		private BooleanProperty isDone;

		ReviewEntry(int id, int classID, String className, Double lectID, LocalDate lecture, LocalDate deadline, double hr, boolean isDone) {
			this.id = id;
			this.classID = classID;
			this.className = className;
			this.lectID = lectID;
			this.lecture = lecture;
			this.deadline = deadline;
			this.hr = hr;
			this.isDone = new SimpleBooleanProperty(isDone);
			this.isDone.addListener((observable, oldValue, newValue) -> {
				if (oldValue != newValue)
				{
					System.out.println(">" + oldValue + "-->" + newValue);
					this.isDone.set(newValue); //flip value locally
					g.runSQL("update " + tableName + " set isDone = " + newValue + " where id = " + id, true); //and remotely
				}
			});
		}

		public int getId() {return id;}
		
		public int getClassID() {return classID;}
		
		public String getClassName() {return className;}
		
		public Double getLectID() {return lectID;}
		
		public LocalDate getDeadline() {return deadline;}
		
		public LocalDate getLecture() {return lecture;}
		
		public double getHr() {return hr;}
		
		public void setHr(Double newValue) {
			hr = newValue;
			g.runSQL("update " + tableName + " set hr = " + newValue + " where id = " + id, true);
		}
		
		public boolean getIsDone() {return isDone.get();}

		public BooleanProperty getIsDoneProperty() {
			return isDone;
		}
		
		

	}

}
