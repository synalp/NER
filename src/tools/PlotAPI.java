/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author rojasbar
 */
public class PlotAPI {
  
    private XYSeries data;
    
    public PlotAPI(String title,String xlabel, String ylabel){
        data = new XYSeries(title);
        
        XYSeriesCollection objDataset = new XYSeriesCollection(data);

        JFreeChart objChart = ChartFactory.createTimeSeriesChart(
                data.getDescription(),     //Chart title
                xlabel,     //Domain axis label
                ylabel,         //Range axis label
                objDataset,         //Chart Data 
                true,             // include legend?
                true,             // include tooltips?
                false             // include URLs?
                );

                NumberAxis x = new NumberAxis();
                x.setTickUnit(new NumberTickUnit(1));

        objChart.getXYPlot().getDomainAxis().setTickLabelsVisible(false);
        
        ChartFrame frame = new ChartFrame(title, objChart);
        frame.pack();
        frame.setVisible(true); 
        objChart.createBufferedImage(100, 100);
    }
    
    public void addPoint(final double x, final double y) {
    	if (data!=null)
    		data.add(x,y);
//        if (y>ymax) ymax=y;
//        if (y<ymin) ymin=y;
//        double y0=ymin-(ymax-ymin)*0.2-0.01;
//        double y1=ymax+(ymax-ymin)*0.2+0.01;
//        yaxis.setRange(y0, y1);
//        yaxis.setTickLabelsVisible(true);
//        double tu = (y1-y0)/4.;
//        yaxis.setTickUnit(new NumberTickUnit(tu));
    }    


}
