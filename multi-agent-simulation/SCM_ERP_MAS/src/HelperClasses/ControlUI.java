package HelperClasses;

import Agents.GUIController;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class ControlUI {

    public static ControlUI Instance;
    private Map<String, String> ProductPrices = new HashMap<>();

    private Map<String, String> Storage = new HashMap<>();

    private String LastProducedProduct = "";

    private String LastProducedProductDisplay = "";

    private ArrayList<String> infoLog = new ArrayList<>();

    private Timer UITimer;

    private GUIController controller;

    private int startDate = 0;
    private int endDate = 0;

    private int infoLogSizeLimit = 30;

    private JProgressBar progressBar;

    private JTextPane productPricesText;

    private JTextPane actionLog;

    private JTextPane storageText;



    public ControlUI(GUIController ctrl){
        controller = ctrl;
        if(Instance == null){
            Instance = this;
        }
        else{
            RuntimeException e = new RuntimeException("Singleton was Instantiated Twice!");
            e.printStackTrace();
            throw e;
        }
        CreateWindow();
    }

    public void SetEndDay(int endday){
        endDate = endday;
    }
    public void SetStartDay(int startday){
        startDate = startday;
    }

    public void ChangeLastProduced(String input){
        if(!input.equals(LastProducedProduct)){
            LastProducedProductDisplay = LastProducedProduct + " -> " + input;
        }
        else{
            LastProducedProductDisplay = input;
        }
        LastProducedProduct = input;
    }

    public void AddInfoLogEntry(String input){
        infoLog.add(input);
    }

    public void UpdateProductPrices(String Product, String Price){
        ProductPrices.put(Product, Price);
    }

    public void UpdatePriceTextBox(){
        StringBuilder pricestr = new StringBuilder();
        pricestr.append("Current Product Prices: ").append("\n");
        for(var item: ProductPrices.keySet().stream().sorted().toList()){
            pricestr.append(item).append(" : ").append(ProductPrices.get(item)).append("\n");
        }
        productPricesText.setText(pricestr.toString());
    }

    public void UpdateStorage(String StorageItem, String Stock){
        Storage.put(StorageItem, Stock);
    }

    private void UpdateStorageTextBox(){
        StringBuilder storagestr = new StringBuilder();
        storagestr.append("Currently Producing: ").append(LastProducedProductDisplay).append("\n");
        for(var item: Storage.keySet().stream().sorted().toList()){
            storagestr.append(item).append(" : ").append(Storage.get(item)).append("\n");
        }
        storageText.setText(storagestr.toString());
    }

    public void UpdateDay(int currentDay){
        float currDayf = currentDay - startDate;
        float endDayf = endDate - startDate;
        float progressf = (currDayf / endDayf) * 100f;
        int progress = Math.round(progressf);

        progressBar.setValue(progress);
        progressBar.setString("Day " + (currentDay - startDate) + " of " + (endDate - startDate));
    }

    private void UpdateInfoLogText(){

        if (infoLog.size() > infoLogSizeLimit){
            if ((infoLog.size() - infoLogSizeLimit) > 0) {
                infoLog.subList(0, (infoLog.size() - infoLogSizeLimit)).clear();
            }
        }
        StringBuilder infoStr = new StringBuilder();
        infoStr.append("Current Actions:\n");
        for (var x: infoLog) {
            infoStr.append(x).append("\n");
        }
        actionLog.setText(infoStr.toString());
    }

    private void UpdateUI(){
        UpdatePriceTextBox();
        UpdateInfoLogText();
        UpdateStorageTextBox();
        UITimer.restart();
    }


    private void CreateWindow() {
        File uiImagesPath = new File(getClass().getClassLoader().getResource("UIImages").getPath());
        List<Image> guiImages = new ArrayList<>();
        for (var x: uiImagesPath.listFiles()){
            guiImages.add(Toolkit.getDefaultToolkit().getImage(x.getPath()));
        }
        JFrame frame = new JFrame("SCM_ERP_MAS Info Console");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280,720);
        frame.setIconImages(guiImages);
        frame.setLocationRelativeTo(null);

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(frame.getWidth(), 50));
        progressBar.setString("Day X of X");
        progressBar.setStringPainted(true);
        frame.add(progressBar, BorderLayout.PAGE_START);

        productPricesText = new JTextPane();
        productPricesText.setPreferredSize(new Dimension(frame.getWidth()/3, frame.getHeight()));
        productPricesText.setBorder(BorderFactory.createLineBorder(Color.black));
        productPricesText.setEditable(false);
        frame.add(productPricesText, BorderLayout.LINE_START);

        actionLog = new JTextPane();
        actionLog.setPreferredSize(new Dimension(frame.getWidth()/3, frame.getHeight()));
        actionLog.setBorder(BorderFactory.createLineBorder(Color.black));
        actionLog.setEditable(false);
        frame.add(actionLog, BorderLayout.CENTER);
        storageText = new JTextPane();
        storageText.setPreferredSize(new Dimension(frame.getWidth()/3, frame.getHeight()));
        storageText.setBorder(BorderFactory.createLineBorder(Color.black));
        storageText.setEditable(false);
        frame.add(storageText, BorderLayout.LINE_END);

        JButton startBtn = new JButton();
        startBtn.setText("Start/Stop");
        startBtn.addActionListener(e -> controller.startStop());
        frame.add(startBtn, BorderLayout.PAGE_END);

        UITimer = new Timer(1000, e -> UpdateUI());
        UITimer.start();

        frame.setVisible(true);

    }

}
