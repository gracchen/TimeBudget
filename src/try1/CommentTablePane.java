package try1;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.*;

public class CommentTablePane {
	private GUI g;
	private Connect c;
	private CommentTable commentTable;
	private String tableName;
	private TableView<CommentEntry> commentTableView;
	private ScrollPane pane;

	public void addComment(String body) {
		LocalDate today = LocalDate.now();
		LocalTime now = LocalTime.now();
		//INSERT INTO comments (Comment, Post_Date, Post_Time) VALUES("test2", "2023-05-28", "07:25:00");
		g.runSQL(String.format("INSERT INTO %s (Comment, Post_Date, Post_Time) VALUES(\"%s\", \"%s\", \"%s\");",tableName,body,today,now), false);
		commentTable.loadTable(); //refresh table
	}
	public CommentTablePane(String name, GUI g) {
		this.g = g;
		this.c = g.c;
		tableName = name;
		commentTable = new CommentTable();
		commentTableView = commentTable.createTableView();
		commentTable.loadTable();
		pane = new ScrollPane(commentTableView);
		pane.setFitToWidth(true);
		pane.setFitToHeight(true);
	}
	
	public ScrollPane getScrollPane() {
		return pane;
	}

	private class CommentTable {
		private ObservableList<CommentEntry> data;
		private TableColumn<CommentEntry, String> commentColumn;
		private TableView<CommentEntry> tableView;
		private TableColumn<CommentEntry, Date> dateColumn;
		private TableColumn<CommentEntry, Time> timeColumn;
		
		public TableView<CommentEntry> createTableView() {
			data = FXCollections.observableArrayList();
			tableView = new TableView<CommentEntry>(data);

			TableColumn<CommentEntry, Integer> idColumn = new TableColumn<>("ID");
			idColumn.setCellValueFactory(new PropertyValueFactory<>("ID"));
			idColumn.setReorderable(false);
			idColumn.setResizable(false);
			idColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.03));
			
			commentColumn = new TableColumn<>("Comment");
			commentColumn.setCellValueFactory(new PropertyValueFactory<>("Comment"));
			commentColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.81));
			setCellFactory();
			commentColumn.setReorderable(false);
			commentColumn.setResizable(false);

			dateColumn = new TableColumn<>("Post_Date");
			dateColumn.setCellValueFactory(new PropertyValueFactory<>("Post_Date"));
			dateColumn.setReorderable(false);
			dateColumn.setResizable(false);
			dateColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.08));
			dateColumn.setSortType(TableColumn.SortType.DESCENDING);
			

			timeColumn = new TableColumn<>("Post_Time");
			timeColumn.setCellValueFactory(new PropertyValueFactory<>("Post_Time"));
			timeColumn.setReorderable(false);
			timeColumn.setResizable(false);
			timeColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.08));
			timeColumn.setSortType(TableColumn.SortType.DESCENDING);

			tableView.getColumns().addAll(idColumn, commentColumn, dateColumn, timeColumn);
			tableView.setEditable(false); 
			tableView.setSelectionModel(null);
			return tableView;
		}

	    private void setCellFactory() {
	    	commentColumn.setCellFactory(f -> {
	            TableCell<CommentEntry, String> cell = new TableCell<CommentEntry, String>() {
	                Text text = new Text();

	                @Override
	                protected void updateItem(String item, boolean empty) {
	                    super.updateItem(item, empty);
	                    if (empty) {
	                        setGraphic(null);
	                        return;
	                    }
	                    text.setWrappingWidth(getTableColumn().getWidth() - 10);
	                    text.setText(item);
	                    setGraphic(text);
	                }
	            };
	            return cell;
	        });
	    }
		
		public void addRow(CommentEntry e) {
			data.add(e);
		}

		public void loadTable() {
			data.clear();
			//loadTable(workTableSortedByCol);
			g.runSQL("select * from " + tableName + ";", false);
			try {
				while(c.rs.next())
					data.add(new CommentEntry(c.rs.getInt("ID"),c.rs.getString("Comment"), c.rs.getDate("Post_Date"), c.rs.getTime("Post_Time")));

				//System.out.printf("%s:%s:%s\n", c.rs.getString("id"), c.rs.getString("name"), c.rs.getString("fixed"));
				tableView.getSortOrder().addAll(dateColumn,timeColumn);
				tableView.sort();
				//data.sort(Comparator.comparing(CommentEntry::getPost_Date).thenComparing(CommentEntry::getPost_Time));
				//tableView.setItems(data);
				System.out.println("successfully imported mySQL commentsTable to JTable");
			} catch (SQLException e) {e.printStackTrace(); System.err.println("load commentsTable failed");}
		}
	}


	public class CommentEntry {
		private int ID;
		private String Comment;
		private Date Post_Date;
		private Time Post_Time;

		CommentEntry(int ID, String Comment, Date Post_Date, Time Post_Time) {
			this.ID = ID;
			this.Comment = Comment;
			this.Post_Date = Post_Date;
			this.Post_Time = Post_Time;
		}

		public int getID() {return ID;}
		
		public String getComment() {return Comment;}
		
		public Date getPost_Date() {return Post_Date;}
		
		public Time getPost_Time() {return Post_Time;}
	}

}
