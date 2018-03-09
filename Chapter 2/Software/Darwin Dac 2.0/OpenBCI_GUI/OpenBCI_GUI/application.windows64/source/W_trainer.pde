class W_Trainer extends Widget {

    //to see all core variables/methods of the Widget class, refer to Widget.pde
    //put your custom variables here...

    Button trainButton;
    Button clearTrainButton;
    Button testButton;
    Button cycleModeButton;
    
    boolean train_positive = false;
    boolean testing = false;
    boolean display_testing = false;
    boolean showColor = false;
    
    float positive_training_alpha_avr = 0.0;
    float positive_training_alpha_data = 0.0;
    float negitive_training_alpha_avr = 0.0;
    float negitive_training_alpha_data = 0.0;
    
    float positive_training_beta_avr = 0.0;
    float positive_training_beta_data = 0.0;
    float negitive_training_beta_avr = 0.0;
    float negitive_training_beta_data = 0.0;
    
    float lookleft = 0.0;
    float lookleft1 = 0.0;
    
    
    

    int train_delay = 4000;
    int last_delay = 0;

    int positive_training_counter = 0;
    int negitive_training_counter = 0;
    int test_positive_counter = 0;


    color t1 = color(255, 0, 0);
    color t2 = color(0, 0, 0);

    color[] colors = {
        t1,
        t2
    };

    PApplet parent;



    W_Trainer(PApplet _parent) {
        super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        parent = _parent;




    }



    void update() {
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

        //put your code here...

        //processTripps();
    }

    void draw() {


        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
        pushStyle();
        noStroke();
        fill(0);
        rect(x, y, w, h);


        trainButton = new Button((int)(x) + 50, (int)(y) - navHeight + 2, 58, navHeight - 6, "Train", fontInfo.buttonLabel_size);
        if (training) {
            trainButton.setColorNotPressed(color(0, 255, 0));
        } else {
            trainButton.setColorNotPressed(color(57, 128, 204));
        }
        trainButton.textColorNotActive = color(0);
        trainButton.setCornerRoundess((int)(navHeight - 6));
        trainButton.setHelpText("Click this button to connect to Darwin Visual Matrix");
        trainButton.draw();
        
        
        clearTrainButton = new Button((int)(x) + 120, (int)(y) - navHeight + 2, 58, navHeight - 6, "Clear", fontInfo.buttonLabel_size);
        clearTrainButton.setColorNotPressed(color(57, 128, 204));
       
        clearTrainButton.textColorNotActive = color(0);
        clearTrainButton.setCornerRoundess((int)(navHeight - 6));
        clearTrainButton.setHelpText("Click this button to connect to Darwin Visual Matrix");
        clearTrainButton.draw();
        
        
        if (trainingMode) {
            cycleModeButton = new Button((int)(x) + (int)(w) / 2 - 10, (int)(y) - navHeight + 2, 58, navHeight - 6, "Positive", fontInfo.buttonLabel_size);
            cycleModeButton.setColorNotPressed(color(0, 255, 0));
        } else {
            cycleModeButton = new Button((int)(x) + (int)(w) / 2 - 10, (int)(y) - navHeight + 2, 58, navHeight - 6, "Negitive", fontInfo.buttonLabel_size);
            cycleModeButton.setColorNotPressed(color(57, 128, 204));
        }
        cycleModeButton.textColorNotActive = color(0);
        cycleModeButton.setCornerRoundess((int)(navHeight - 6));
        cycleModeButton.setHelpText("Click this button to connect to Darwin Visual Matrix");
        cycleModeButton.draw();

        testButton = new Button((int)(x) + (int)(w)  - 100, (int)(y) - navHeight + 2, 58, navHeight - 6, "Test", fontInfo.buttonLabel_size);
        if (testing) {
            testButton.setColorNotPressed(color(0, 255, 0));
        } else {
            testButton.setColorNotPressed(color(57, 128, 204));
        }
        testButton.textColorNotActive = color(0);
        testButton.setCornerRoundess((int)(navHeight - 6));
        testButton.setHelpText("Click this button to connect to Darwin Visual Matrix");
        testButton.draw();
        
        
        



        stroke(1, 18, 41, 125);
        
        if (training){
          if (trainingMode){
            train_positive();
          }
          else {
            train_negitive();
          }
        }
        
        if (testing){
          if (trainingMode){
            test_positive();
          }
          else {
            test_negitive();
          }
        }
        

      
        // float rx = x, ry = y + 2* navHeight, rw = w, rh = h - 2*navHeight;
        float rx = x, ry = y, rw = w, rh = h;
        float scaleFactor = 1.0;
        float scaleFactorJaw = 1.5;
        int rowNum = 4;


        int index = 0;
        float currx, curry;
        //new


        popStyle();

    }
    
    void test_results(){
      float margin = .07;
      float highMargin = 1 + margin;
      float lowMargin = 1 - margin;
      
      
      if ((dataProcessing.headWidePower[ALPHA] > positive_training_alpha_avr * lowMargin) && (dataProcessing.headWidePower[ALPHA] < positive_training_alpha_avr * highMargin) && 
          (dataProcessing.headWidePower[BETA] > positive_training_beta_avr * lowMargin) && (dataProcessing.headWidePower[BETA] < positive_training_beta_avr * highMargin)) {
        println("training alpha avr: " + positive_training_alpha_avr);
        println("current alpha reading: " + dataProcessing.headWidePower[ALPHA]);
        println("training beta avr: " + positive_training_alpha_avr);
        println("current beta reading: " + dataProcessing.headWidePower[BETA]);
        test_positive_counter +=1;
        
      }
      else{
        test_positive_counter = 0;
      }
      if (dataProcessing.headWidePower[ALPHA] < positive_training_alpha_avr * lowMargin) println("alpha too low ");
      if (dataProcessing.headWidePower[BETA] < positive_training_beta_avr * lowMargin) println("beta too low ");
      
      if (dataProcessing.headWidePower[ALPHA] > positive_training_alpha_avr * highMargin) println("alpha too high ");
      if (dataProcessing.headWidePower[BETA] > positive_training_beta_avr * highMargin) println("beta too high ");
      
      
      if (test_positive_counter >= 2){
        playVoice(0);
        test_positive_counter = 0;
      }
      
      if (dataProcessing.data_std_uV[0] > lookleft * .95 && dataProcessing.data_std_uV[0] < lookleft * 1.05) println("looking Left");
         
      
    }
    
    void test_negitive(){
      fill(t2);
      noStroke();
      rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
      stroke(126);
      line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
      line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
            
      test_results();
        
    }
    
    void test_positive(){
      if (millis() - train_delay > last_delay) {
          showColor = !showColor;
          last_delay = millis();
      }
      if (showColor) {
          fill(t1);
          noStroke();
          rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
          stroke(126);
          line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
          line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
      } else {
          fill(t1);
          noStroke();
          rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
          stroke(126);
          line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
          line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
          //marker_indicater = 0;
      }
      test_results();
   
    }
    
    void train_negitive(){
      negitive_training_counter += 1;
      negitive_training_alpha_data += dataProcessing.headWidePower[ALPHA];
      negitive_training_alpha_avr = negitive_training_alpha_data / negitive_training_counter;
      
      negitive_training_beta_data += dataProcessing.headWidePower[BETA];
      negitive_training_beta_avr = negitive_training_beta_data / negitive_training_counter;
      
      fill(t2);
      noStroke();
      rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
      stroke(126);
      line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
      line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
      
      
      println("TN - Positive Training avr: " + positive_training_alpha_avr);
      println("TN - Negitive Training avr: " + negitive_training_alpha_avr);
      
    }
    
    void train_positive(){
     
      
      

      if ((millis() - train_delay > last_delay) && (!testing)) {
          showColor = !showColor;
          last_delay = millis();
      }
      if (showColor) {
          fill(t1);
          noStroke();
          rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
          stroke(126);
          line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
          line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
          positive_training_counter += 1;
          positive_training_alpha_data += dataProcessing.headWidePower[ALPHA];
          positive_training_alpha_avr = positive_training_alpha_data / positive_training_counter;
      
          positive_training_beta_data += dataProcessing.headWidePower[BETA];
          positive_training_beta_avr = positive_training_beta_data / positive_training_counter;
          marker_indicater = 1;
          println("TP - Positive Training avr: " + positive_training_alpha_avr);
          if (dataProcessing.data_std_uV[0] > lookleft) lookleft = dataProcessing.data_std_uV[0];
          if (dataProcessing.data_std_uV[1] > lookleft1) lookleft1 = dataProcessing.data_std_uV[1];
      } else {
          fill(t2);
          noStroke();
          rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
          stroke(126);
          line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
          line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
          
          negitive_training_counter += 1;
          negitive_training_alpha_data += dataProcessing.headWidePower[ALPHA];
          negitive_training_alpha_avr = negitive_training_alpha_data / negitive_training_counter;
      
          negitive_training_beta_data += dataProcessing.headWidePower[BETA];
          negitive_training_beta_avr = negitive_training_beta_data / negitive_training_counter;
          marker_indicater = 0;
          println("TP - Negitive Training avr: " + negitive_training_alpha_avr);
      }
      
     
        
      
    }

    void screenResized() {
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)


    }

    void mousePressed() {
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
        if (trainButton.isMouseHere()) {
            trainButton.setIsActive(true);
            println("Train Button pressed");
        } else trainButton.setIsActive(false);


        if (testButton.isMouseHere()) {
            testButton.setIsActive(true);
            println("Test Button pressed");
        } else testButton.setIsActive(false);
        
        if (cycleModeButton.isMouseHere()) {
            cycleModeButton.setIsActive(true);
            println("Cycle Mode Button pressed");
        } else cycleModeButton.setIsActive(false);

    }

    void mouseReleased() {
        super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)
        if (trainButton != null && trainButton.isMouseHere()) {
            //do some function
            training = !training;
            if (training){
              testing = false;
            } else{
              marker_indicater = 0;}
            trainButton.wasPressed = true;
            verbosePrint("Training");
        }
        if (testButton != null && testButton.isMouseHere()) {
            //do some function
            testing = !testing;
            if (testing) training = false;
            testButton.wasPressed = true;
            verbosePrint("Testing");
        }
        
        if (cycleModeButton != null && cycleModeButton.isMouseHere()) {
            //do some function
            trainingMode = !trainingMode;            
            cycleModeButton.wasPressed = true;
            verbosePrint("Switching Modes");
        }
        
        if (clearTrainButton != null && clearTrainButton.isMouseHere()){
            verbosePrint("Clear Training Data");
            positive_training_alpha_avr = 0.0;
            positive_training_alpha_data = 0.0;
            negitive_training_alpha_avr = 0.0;
            negitive_training_alpha_data = 0.0;
            
            positive_training_beta_avr = 0.0;
            positive_training_beta_data = 0.0;
            negitive_training_beta_avr = 0.0;
            negitive_training_beta_data = 0.0;
        }
    }



    void playVoice(int channel) {
        try {
            voice.pause();
        } catch (Exception e) {
            e.printStackTrace();
        }



        switch (channel) {

            case 0:
                voice = soundMinim.loadFile(dataPath("m_yes.mp3"));
                voice.play();
                watchBeat = false;
                break;

        }

    }




};