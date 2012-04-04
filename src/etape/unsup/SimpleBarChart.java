package etape.unsup;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class SimpleBarChart extends JPanel {
	private double[] value;
	private String[] languages;
	private String title;

	public SimpleBarChart(double[] val, String[] lang, String t) {
		languages = lang;
		value = val;
		title = t;
	}
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		if (value == null || value.length == 0)
			return;
		double minValue = 0;
		double maxValue = 0;
		for (int i = 0; i < value.length; i++) {
			if (minValue > value[i])
				minValue = value[i];
			if (maxValue < value[i])
				maxValue = value[i];
		}
		Dimension dim = getSize();
		int clientWidth = dim.width;
		int clientHeight = dim.height;
		int barWidth = clientWidth / value.length;
		Font titleFont = new Font("Book Antiqua", Font.BOLD, 15);
		FontMetrics titleFontMetrics = graphics.getFontMetrics(titleFont);
		Font labelFont = new Font("Book Antiqua", Font.PLAIN, 10);
		FontMetrics labelFontMetrics = graphics.getFontMetrics(labelFont);
		int titleWidth = titleFontMetrics.stringWidth(title);
		int q = titleFontMetrics.getAscent();
		int p = (clientWidth - titleWidth) / 2;
		graphics.setFont(titleFont);
		graphics.drawString(title, p, q);
		int top = titleFontMetrics.getHeight();
		int bottom = labelFontMetrics.getHeight();
		if (maxValue == minValue)
			return;
		double scale = (clientHeight - top - bottom) / (maxValue - minValue);
		q = clientHeight - labelFontMetrics.getDescent();
		graphics.setFont(labelFont);
		for (int j = 0; j < value.length; j++) {
			int valueP = j * barWidth + 1;
			int valueQ = top;
			int height = (int) (value[j] * scale);
			if (value[j] >= 0)
				valueQ += (int) ((maxValue - value[j]) * scale);
			else {
				valueQ += (int) (maxValue * scale);
				height = -height;
			}
			graphics.setColor(Color.blue);
			graphics.fillRect(valueP, valueQ, barWidth - 2, height);
			graphics.setColor(Color.black);
			graphics.drawRect(valueP, valueQ, barWidth - 2, height);
			int labelWidth = labelFontMetrics.stringWidth(languages[j]);
			p = j * barWidth + (barWidth - labelWidth) / 2;
			graphics.drawString(languages[j], p, q);
		}
	}
	
	public static int w=350,h=300;
	public static JFrame frame = null;
	
	public static void drawChart(double[] vals) {
		String[] xlabs = new String[vals.length];
		for (int i=0;i<vals.length;i++)
			xlabs[i]=""+vals[i];
		drawChart(vals, xlabs, true);
	}
	public static void drawChart(double[] vals, String[] xlabs, boolean dowait) {
		final Boolean b = true;
		if (frame==null) {
			frame = new JFrame();
			frame.setSize(w, h);
		}
		frame.getContentPane().removeAll();
		frame.getContentPane().add(new SimpleBarChart(vals, xlabs,"chart"));
		WindowListener winListener = new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				frame.dispose();
				synchronized (b) {
					b.notify();
				}
			}
		};
		frame.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				synchronized (b) {
					b.notify();
				}
			}
		});
		frame.addWindowListener(winListener);
		frame.setVisible(true);
		frame.repaint();
		
		if (dowait) {
			synchronized (b) {
				try {
					b.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	public static void drawChart(java.util.List<Double> xvals, String[] xlabs) {
		final Boolean b = true;
		if (frame==null) {
			frame = new JFrame();
			frame.setSize(w, h);
		}
		frame.getContentPane().removeAll();
		double[] vals = new double[xvals.size()];
		for (int i=0;i<vals.length;i++) vals[i]=xvals.get(i);
		frame.getContentPane().add(new SimpleBarChart(vals, xlabs,"chart"));
		WindowListener winListener = new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				frame.dispose();
				synchronized (b) {
					b.notify();
				}
			}
		};
		frame.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				synchronized (b) {
					b.notify();
				}
			}
		});
		frame.addWindowListener(winListener);
		frame.setVisible(true);
		frame.repaint();

		synchronized (b) {
			try {
				b.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setSize(350, 300);
		double[] value= new double[5];
		String[] languages = new String[5];
		value[0] = 1;
		languages[0] = "Visual Basic";

		value[1] = 2;
		languages[1] = "PHP";

		value[2] = 3;
		languages[2] = "C++";

		value[3] = 4;
		languages[3] = "C";

		value[4] = 5;
		languages[4] = "Java";
		frame.getContentPane().add(new SimpleBarChart(value, languages,
				"Programming Languages"));

		WindowListener winListener = new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				System.exit(0);
			}
		};
		frame.addWindowListener(winListener);
		frame.setVisible(true);
	}
}