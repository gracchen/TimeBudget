package try1;
import javax.swing.JFrame;

public class MainTime {
	public static void main(String[] args) {
		GUI window = new GUI();
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		window.setSize(1200,600);
		window.setVisible(true);
	}
}
