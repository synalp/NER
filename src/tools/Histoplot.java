package tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;

public class Histoplot extends JFrame {

	private static final long serialVersionUID = 1L;
	double[] sc;
	private static Histoplot frame = null;
	
	public static void showit(final float[] x) {
		double[] y = new double[x.length];
		for (int i=0;i<y.length;i++) y[i]=x[i];
		showit(y, y.length);
	}

	public static void showit(final double[] x, final int len) {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				if (frame==null) {
					frame = new Histoplot();
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					frame.setBounds(10, 10, 500, 500);
					frame.setTitle("score histogram");
					frame.setVisible(true);
				}
				double[] xx = Arrays.copyOf(x, len);
				frame.sc=xx;

				if (true) {
					// debug: find highest peak
					Arrays.sort(xx);
					int nbest=0; double xbest=Double.NaN;
					for (int i=0;i<xx.length;) {
						int n=1;
						for (int j=i+1;j<xx.length;j++)
							if (xx[j]==xx[i]) ++n;
						if (n>nbest) {
							nbest=n;
							xbest=xx[i];
						}
						i+=n;
					}
					frame.setTitle("score histogram highestpeak "+xbest+" "+nbest);
					frame.repaint();
				}
				
				JFreeChart chart = frame.createChart();

				try {
					//ChartUtilities.saveChartAsPNG(new File("test.png"), chart, 300, 300);
				} catch (Exception e) {
					e.printStackTrace();
				}

				ChartPanel cpanel = new ChartPanel(chart);
				frame.getContentPane().removeAll();
				frame.getContentPane().add(cpanel, BorderLayout.CENTER);
				frame.validate();
				frame.repaint();
				
			}
		});
		t.start();
	}

	private JFreeChart createChart() {
		HistogramDataset dataset = new HistogramDataset();
		int bin = 100;
		
		double scmin=sc[0], scmax=sc[0];
		for (int i=0;i<sc.length;i++)
			if (sc[i]<scmin) scmin=sc[i];
			else if (sc[i]>scmax) scmax=sc[i];
		dataset.addSeries("Scores", sc, bin, scmin-1, scmax+1);
		
		JFreeChart chart = ChartFactory.createHistogram(
				"Histogram Demo",
				null,
				null,
				dataset,
				PlotOrientation.VERTICAL,
				true,
				false,
				false
				);

		chart.setBackgroundPaint(new Color(230,230,230));
		XYPlot xyplot = (XYPlot)chart.getPlot();
		xyplot.setForegroundAlpha(0.7F);
		xyplot.setBackgroundPaint(Color.WHITE);
		xyplot.setDomainGridlinePaint(new Color(150,150,150));
		xyplot.setRangeGridlinePaint(new Color(150,150,150));
		XYBarRenderer xybarrenderer = (XYBarRenderer)xyplot.getRenderer();
		xybarrenderer.setShadowVisible(false);
		xybarrenderer.setBarPainter(new StandardXYBarPainter());
		//  	    xybarrenderer.setDrawBarOutline(false);
		return chart;
	}

}
