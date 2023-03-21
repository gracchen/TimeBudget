package try1;
import javax.swing.JFrame;

public class MainTime {
	public static void main(String[] args) {
		int pollTime = 1 * 1000; //every second
		GUI window = new GUI();
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(300,220);
		window.setVisible(true);
	}
}
