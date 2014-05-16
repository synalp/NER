/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.ScatterRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultMultiValueCategoryDataset;
import org.jfree.data.statistics.MultiValueCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleInsets;

/**
 *
 * @author rojasbar
 */
public class ScatterPlotAPI extends ApplicationFrame
 {
  
    private DefaultMultiValueCategoryDataset data ;
    
    
    public ScatterPlotAPI(String s){
        super(s);
         
        data = new DefaultMultiValueCategoryDataset();
        CategoryPlot categoryplot = new CategoryPlot(data, new CategoryAxis("Category"), new NumberAxis("Value"), new ScatterRenderer());
                categoryplot.setBackgroundPaint(Color.lightGray);
                categoryplot.setDomainGridlinePaint(Color.white);
                categoryplot.setRangeGridlinePaint(Color.white);
                categoryplot.setAxisOffset(new RectangleInsets(4D, 4D, 4D, 4D));
                JFreeChart jfreechart = new JFreeChart(categoryplot);
                jfreechart.setBackgroundPaint(Color.white);
                
                
        JPanel jpanel =new ChartPanel(jfreechart);;
        jpanel.setPreferredSize(new Dimension(500, 270));
        setContentPane(jpanel);
        
        pack();
        setVisible(true);

    }
    
    public void addPoint(final float[] value, int trial) {
    	if (data ==null)
            return;
        ArrayList arraylist = new ArrayList();
        for(int i=0; i<value.length;i++){
            arraylist.add(value[i]);
        }    
            
            data.add(arraylist, "sampled point","trial_"+trial);
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
