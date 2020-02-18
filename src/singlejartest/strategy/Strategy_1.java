package singlejartest.strategy;

import com.dukascopy.api.*;
import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.ITextChartObject;

import java.awt.*;
import java.util.ArrayList;

public class Strategy_1 implements IStrategy {
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    private IChartObjectFactory factory;
    private IChart chart;
    private boolean last_pick_its_max=true;
    private IBar bar_max=null;
    private IBar bar_min=null;
    private double len=0.001;
    int x=10;
    double max_balance=0;
    private double prozent=0.8;
    private ArrayList<Candidat> candidats = new ArrayList();

    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();
         Instrument instrument = Instrument.EURUSD;
        chart = context.getChart(instrument);
        factory = chart.getChartObjectFactory();
        max_balance=context.getAccount().getEquity();

    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onStop() throws JFException {
        chart.removeAll();
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if(period==chart.getSelectedPeriod()){
            if(max_balance< context.getAccount().getEquity()) max_balance=context.getAccount().getEquity();
            else {
                if(max_balance*0.9>context.getAccount().getEquity()){
                    max_balance=context.getAccount().getEquity();
                    try{
                        for (IOrder o : engine.getOrders()){
                            o.close();
                        }
                    }catch(Exception e){
                    }

                }
            }


            //первичная инициализация максимальных баров
            if(bar_max==null) bar_max=askBar;
            if(bar_min==null) bar_min=askBar;

            // ищем 80%
            if(candidats.size()>0) {
                for (int index = 0; index < candidats.size(); ) {
                    if (candidats.get(index).Update(askBar)) index++;
                    else candidats.remove(index);
                }
            }

            // определяем вершину
            if(last_pick_its_max){

                if(bar_max.getHigh()<askBar.getHigh()){
                    bar_max=askBar;
                }

                if(bar_min.getLow()>askBar.getLow()){
                    bar_min=askBar;
                    bar_max=null;
                    return;
                } else {
                    if ((bar_max.getHigh() - bar_min.getLow()) / bar_min.getLow() > len) {
                        last_pick_its_max = false;
                        candidats.add(new Candidat(bar_min, bar_max, true));
                        //chart.add(factory.createSignalUp(askBar.toString(),bar_min.getTime(), bar_min.getLow()*0.9995));
                        ITextChartObject text = factory.createText(askBar.toString(), bar_min.getTime(), bar_min.getLow() * 0.9995);
                        text.setText(String.valueOf(new char[]{8226}), new Font(Font.DIALOG, Font.PLAIN, 20));
                        text.setColor(Color.GREEN);
                        chart.add(text);
                        bar_min = null;

                    }
                }

            }
            else {

                if(bar_min.getHigh()>askBar.getLow()){
                    bar_min=askBar;
                }

                if(bar_max.getHigh()<askBar.getHigh()){
                    bar_min=null;
                    bar_max=askBar;
                } else {
                    if ((bar_max.getHigh() - bar_min.getLow()) / bar_min.getLow() > len) {
                        last_pick_its_max = true;
                        candidats.add(new Candidat(bar_min, bar_max, false));
                        //chart.add(factory.createSignalDown(askBar.toString(),bar_max.getTime(), bar_max.getHigh()));
                        ITextChartObject text = factory.createText(askBar.toString(), bar_max.getTime(), bar_max.getHigh());
                        text.setText(String.valueOf(new char[]{8226}), new Font(Font.DIALOG, Font.PLAIN, 20));
                        text.setColor(Color.RED);
                        chart.add(text);
                        bar_max = null;

                    }
                }
            }

        }

    }

    void open_order(boolean buy, Instrument instrument,IBar bar){
        try {
            /*for (IOrder o : engine.getOrders()){
                o.close();
            }*/

            x++;
            IOrder order;
            if (buy) {
                order= engine.submitOrder("order"+ x, Instrument.EURUSD, IEngine.OrderCommand.BUY, 1.01);
                order.setStopLossPrice(this.bar_min.getLow());
            }
            else  {
                order= engine.submitOrder("order"+ x, Instrument.EURUSD, IEngine.OrderCommand.SELL, 1.01);
                order.setStopLossPrice(this.bar_max.getHigh());
            }

        }catch (Exception e){

        }
    }

    public class Candidat{
        IBar min_bar;
        IBar max_bar;
        double price;
        boolean its_up_wave;

        Candidat(IBar min_bar, IBar max_bar,boolean its_up_wave){
            this.max_bar=max_bar;
            this.min_bar=min_bar;
            this.its_up_wave=its_up_wave;
            if(this.its_up_wave)  price= max_bar.getHigh() - (max_bar.getHigh()-min_bar.getLow())*(prozent);
            else price= max_bar.getHigh() - (max_bar.getHigh()-min_bar.getLow())*(1-prozent);


        }

        boolean Update(IBar bar){
            if(its_up_wave){
                if(price>bar.getLow()){
                    chart.add(factory.createSignalUp(Long.toString(bar.getTime())+"s",bar.getTime(), bar.getLow()));
                    ITextChartObject text = factory.createText(Long.toString(bar.getTime()), bar.getTime(), price);
                    text.setText(Double.toString(price).substring(0,6));
                    text.setColor(Color.GREEN);
                    chart.add(text);
                    if(this.its_up_wave) open_order(true,Instrument.EURUSD,min_bar);
                    else open_order(false,Instrument.EURUSD,max_bar);
                    return false;
                } else {
                    if(bar.getHigh()>=max_bar.getHigh()){
                        max_bar=bar;
                        price= max_bar.getHigh() - (max_bar.getHigh()-min_bar.getLow())*prozent;
                    }
                }
            } else {
                if(price<bar.getHigh()){
                    chart.add(factory.createSignalDown(Long.toString(bar.getTime())+"s",bar.getTime(), bar.getHigh()));
                    ITextChartObject text = factory.createText(Long.toString(bar.getTime()), bar.getTime(), price);
                    text.setText(Double.toString(price).substring(0,6));
                    text.setColor(Color.RED);
                    chart.add(text);
                    if(this.its_up_wave) open_order(true,Instrument.EURUSD,min_bar);
                    else open_order(false,Instrument.EURUSD,max_bar);
                    return false;
                } else {
                    if(bar.getLow()<=min_bar.getLow()){
                        min_bar=bar;
                        price= max_bar.getHigh() - (max_bar.getHigh()-min_bar.getLow())*(1-prozent);
                    }
                }
            }
            return true;
        }

    }
}