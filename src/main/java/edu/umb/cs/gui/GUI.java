/**
 * Copyright (C) 2013 Tokanagrammar Team
 *
 * This is a jigsaw-like puzzle game,
 * except each piece is token from a source file,
 * and the 'complete picture' is the program.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.umb.cs.gui;

import edu.umb.cs.Tokanagrammar;
import edu.umb.cs.api.APIs;
import edu.umb.cs.entity.Category;
import edu.umb.cs.entity.Hint;
import edu.umb.cs.entity.Puzzle;
import edu.umb.cs.gui.screens.SecondaryScreen;
import edu.umb.cs.parser.BracingStyle;
import edu.umb.cs.source.*;
import java.util.*;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Handle game states and also work as a main GUI API.
 * @author Matt
 */
public class GUI {

	public enum GameState {INIT_GUI, START_GAME, FULL_LHS, COMPILING};

        // maximum time allowed for compilation
        private static long TIME_OUT = 10000;

        private static GameBoard gameBoard;
	private static GUITimer timer;
	private static OutputPanel outputPanel;
	private static GameState curGameState;

        /**maps the gameState to a list of active buttons while in this state**/
	// TODO: use Map and List
        private static HashMap<GameState, ArrayList<String>> activeButtons;
	
	private int curDifficulty;

        private List<Category> categories;

        private List<SourceToken> tokenBayTokens;
        
	private List<SourceToken> tokenBoardTokens;
	
	private List<Category> curCategories;
        
        private Iterator<Puzzle> puzzlesIter;

        private Set<Puzzle> puzzles;

        private static BracingStyle curBracingStyle = BracingStyle.ALLMAN;

        /** Used to blur screen on pausing*/
	private static boolean init  = false;
	
	private static boolean inGame;

	private static final GUI gui = new GUI();

        private static Puzzle curPuzzle;
        
        private static int curHint = 0;

        private static ShuffledSource currentSource;
        
        private static final Font defaultFont = new Font(14);
	private GUI(){}
        
        // -----

	/**
	 * GUI uses the singleton pattern.
	 * @return
	 */
	public static GUI getInstance(){
		if(!init){
			setupActiveButtonsTable();
			gameBoard = GameBoard.getInstance();
			outputPanel = OutputPanel.getInstance();
			timer = GUITimer.getInstance();
			init = true;
		}

		return gui;
	}


	//--------------------------------------------------------------------------
	//GAMESTATES

	/**
	 * Initialize the GUI:
	 * gameState is "initGUI"
	 * Welcome the user and give prompt to choose category.
	 */
	public void gameState_initGUI(){

		inGame = false;
		curGameState = GameState.INIT_GUI;
		blurOff();

		printWelcomeMessage();

		initButtons(activeButtons.get(curGameState));
	}

    /**
     * Start the game gameState is "startGame" The user has selected a category
     * (or categories) and pressed "start".
     */
    public void gameState_startGame(boolean newGame)
    {
        curGameState = GameState.START_GAME;
        blurOff();

        //initialization of new start of game
        if (!inGame)
        {
            if (newGame || curPuzzle == null || currentSource == null)
            {
                SourceFile orig = null;
                try
                {
                    // wrap-around to the beginning of the collection
                    if (!puzzlesIter.hasNext())
                        puzzlesIter = puzzles.iterator();
                    curPuzzle = puzzlesIter.next();
                    orig = curPuzzle.getSourceFile(curBracingStyle);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    outputPanel.compilerMessage("Error retrieving puzzles: " + ex.getMessage());
                }

                if (orig != null)
                {
                    currentSource = edu.umb.cs.api.APIs.shuffle(orig, curDifficulty);

                    tokenBayTokens = currentSource.getRemovedTokens();

                    gameBoard.initTokenBoard(currentSource.getShuffledSource());
                    gameBoard.initTokenBay(RHSTokenIconizer.iconizeTokens(tokenBayTokens));

                    outputPanel.clear();
                    printCategoryAndDifficultyMessage();

                    // Some message on the puzzle
                    if (orig != null)
                    {
                        outputPanel.infoMessage("Total (removable) tokens: " + currentSource.totalRemovable());
                        outputPanel.infoMessage("Removed: " + currentSource.removedCount()
                                                + "(" + curDifficulty + "%)");
                    }
                }
            }
            else
            {
                tokenBayTokens = currentSource.getRemovedTokens();

                    gameBoard.initTokenBoard(currentSource.getShuffledSource());
                    gameBoard.initTokenBay(RHSTokenIconizer.iconizeTokens(tokenBayTokens));

                    //outputPanel.clear();
                    //printCategoryAndDifficultyMessage();

            }
        }

        if (GameBoard.getInstance().isRHSempty())
        {
            enableCompileButton();
            curGameState = GameState.FULL_LHS;
        }

        timer.start();
        inGame = true;

        initButtons(activeButtons.get(curGameState));
    }

	//--------------------------------------------------------------------------
	//UTIL
	
	/**
	 * Pause the game
	 * All secondary screens go here.
	 */
	public void pauseGame(SecondaryScreen screen){
		timer.stop();
		blurOn();
		screen.setupScreen();
	}
	
	/**
	 * Blurs the main frame of the GUI (it's AnchorPane).
	 */
	public void blurOn(){
		AnchorPane mainScreen = Tokanagrammar.getAnchorPane();
		ObservableList<Node> screenComponents = mainScreen.getChildren();
		
		BoxBlur bb = new BoxBlur();
		bb.setIterations(3);
		for(Node node: screenComponents)
			node.effectProperty().set(bb);
	}
	
	/**
	 * Turn blur off the main frame.
	 */
	public void blurOff(){
		AnchorPane mainScreen = Tokanagrammar.getAnchorPane();
		ObservableList<Node> screenComponents = mainScreen.getChildren();
		
		for(Node node: screenComponents)
			node.effectProperty().set(null);
	}
	
	/**
	 * Reset the Game
	 * Warning: This reboots the game completely!
	 * If you want to place the orig rhs and lhs tokens
	 * back to their original state, use refresh.
	 * 
	 * GameState is Reset
	 */
	public void resetGame(){
                LHSTokenIconizer.resetIndex();
                RHSTokenIconizer.resetIndex();
		gameBoard.resetTokenBay();
		gameBoard.resetTokenBoard();
		timer.reset();
		outputPanel.clear();
	}
	
	/**
	 * Refresh the Game
	 * Places all original tokens back to their
	 * original place -- DOES NOT RESET TIMER.
	 * 
	 * GameState is Refresh
	 */
        public void refreshGame()
        {													//TODO @mhs this is same as reset for now.
            LHSTokenIconizer.resetIndex();
            RHSTokenIconizer.resetIndex();
            gameBoard.resetTokenBay();
            gameBoard.resetTokenBoard();
            inGame = false;
            gameState_startGame(false);
        }
	
	/**
	 * Skips the current board and goes to the next. 							//TODO backend
	 */
	public void skipPuzzle(){
		System.out.println("<<<Back end for getting the next puzzle>>>");		//TODO backend
		
		outputPanel.clear();

		printCategoryAndDifficultyMessage();
		
		resetGame();
		inGame = false;
		gameState_startGame(true);
	}
	
	/**
	 * Called when all of the RHS is empty.
	 */
	public void enableCompileButton(){
		System.out.println("ENABLE COMPILE BUTTON CALLED");
		initButtons(activeButtons.get(GameState.FULL_LHS));
	}
	
	/**
	 * Called when there is at least one iToken on the RHS.
	 */
	public void disableCompileButton(){
		initButtons(activeButtons.get(GameState.START_GAME));
	}
	
	/**
	 * Called after all the RHS tokens are on the LHS and the compile btn
	 * was enabled, then pressed.
	 */
	public void compileNewSource(){												//TODO Backend specialty!  
		List<LHSIconizedToken> tokenList;										//For now, just print formated source code!
		GameBoard gb = GameBoard.getInstance();		
		tokenList = gb.getTokenBoardItokens();

                System.out.println("\n\nCompiling New Source Code.");

                //Stop the timer to save the user precious ms while
                //compiling -- restart it immediatly below if there are errors.
                timer.stop();	

                // build the content of the file
                // (ie., just dump it to a string)
                final StringBuilder bd = new StringBuilder();
                for (LHSIconizedToken tk : tokenList)
                {
                    SourceToken srcTk = tk.getSourceToken();
                    if (srcTk.kind() != SourceTokenKind.EMPTY)
                        bd.append(tk.getSourceToken().image());
                }
                enableStopButton();
                
                final JFrame waitPopup = new WaitWindow();
                waitPopup.setVisible(true);
                compileTask.src = bd.toString();
                new Thread(compileTask).start();
                try
                {
                    // wait for compilation
                    synchronized(COMPILE_LOCK)
                    {
                        COMPILE_LOCK.wait(TIME_OUT);
                    }
                    System.out.println("done compiling:");
                    Output out = compileTask.out;
                    if (out == null)
                    {
                        waitPopup.dispose();
                        blurOff();
                        outputPanel.compilerMessage("Compilation took too long! Please try again!");
                    }
                    else if (out.isError())
                    {
                        outputPanel.compilerMessage("The program has the following errors:");
                        outputPanel.compilerMessage(out.getOuput());
                        timer.start();
                    }
                    else
                    {
                        if (out.getOuput().equals(curPuzzle.getExpectedOutput()))
                        {
                            outputPanel.infoMessage("Congratulations! You have successfully solved the puzzle!");
                            outputPanel.infoMessage("The output is:\n-----");
                            outputPanel.outputText(out.getOuput());
                            outputPanel.infoMessage("-----");
                        }
                        else
                        {
                            outputPanel.compilerMessage("Your program's output does NOT match the expected! Please try again");
                        }
                        // TODO: record score
                        timer.stop();
                    }
                    waitPopup.dispose();
                    disableStopButton();
                    blurOff();
                }
                catch (InterruptedException ex)
                {
                    outputPanel.compilerMessage("Something went wrong!");
                    ex.printStackTrace();
                    
                    // recover
                    waitPopup.dispose();
                    blurOff();
                }
	}
	
	/**
	 * Enable stop button.
	 * Only used in "compile state"
	 */
	public void enableStopButton(){
		initButtons(activeButtons.get(GameState.COMPILING));
	}
	
	/**
	 * Disable stop button.
	 * Fired when returning from "compile state".
	 */
	public void disableStopButton(){
		initButtons(activeButtons.get(GameState.START_GAME));
	}
	
	/**
	 * The compile button is turned on while compiling only.
	 * Use this as a fail safe to stop compiling (avoids stack overflow etc).
	 */
	public void stopCompile(){
//		Text text = new Text("Stop button needs to be hooked up -- enable WHILE COMPILING ONLY");
//		System.out.println("Stop button needs to be hooked up -- enable WHILE COMPILING ONLY");
//		outputPanel.writeNodes(text);
		
		//Last, send the user back to the full LHS pseudo state and restart the timer.
		initButtons(activeButtons.get(GameState.FULL_LHS));
		timer.start();
	}
	
	
	//--------------------------------------------------------------------------
	//Static message printing
	
	
	private void printWelcomeMessage(){
		
		Text welcomeText = new Text("Welcome to Tokanagrammar, Java Edition! ");
		welcomeText.setFont(defaultFont);
		outputPanel.writeNodes(welcomeText);
		
		Text categoryText = new Text("Please select a category ");
		categoryText.setFont(defaultFont);
		Image img = new Image(OutputPanel.class.
				getResourceAsStream("/images/ui/categoryButton_console_display_size.fw.png"));
		ImageView imgView = new ImageView(img);
		Text text = new Text(" to continue.");
		text.setFont(defaultFont);
		outputPanel.writeNodes(categoryText, imgView, text);
	}
	
	private void printCategoryAndDifficultyMessage(){
		/*
		 * Message to user	"Category <categories> has been selected on difficulty <difficulty>
		 * 					 Hints: <hints>
		 */
		StringBuilder concatCategories = new StringBuilder();
		for(int i=0; i< curCategories.size(); i++)
                    concatCategories.append(' ').append(curCategories.get(i)).append(',');
                Label categoryText;
                // chop off the last comma
                int len = concatCategories.length();
                if (len != 0
                        && concatCategories.charAt(len - 1) == ',')
                    categoryText = new Label(concatCategories.substring(0, len -1));
                else
                    categoryText = new Label(concatCategories.toString());
                outputPanel.infoMessage((categories.size() > 1
                                            ? "Categories: "
                                            : "Category")
                                        +concatCategories);
                
                StringBuilder diffBd = new StringBuilder("Difficulty: ");
                diffBd.append(curDifficulty);
                if(curDifficulty >= 0 && curDifficulty <= 32)
                    diffBd.append("(EASY)");
		else if(curDifficulty >= 33 && curDifficulty <= 64)
                    diffBd.append("(MEDIUM)");
		else if(curDifficulty >= 65 && curDifficulty <= 90)
                    diffBd.append("(HARD)");
		else if(curDifficulty >= 91 && curDifficulty <= 100)
                    diffBd.append("(INSANE)");
                outputPanel.infoMessage(diffBd.toString());
		
                StringBuilder hintsBd = new StringBuilder("Hint: ");
                
                List<Hint> hints = curPuzzle.getHints();
                hintsBd.append(hints.isEmpty()
                                 ? "<NO hints available!>"
                                 :curPuzzle.getHints().get(curHint).getHintContent());
                outputPanel.infoMessage(hintsBd.toString());
	}

	
	
	//--------------------------------------------------------------------------
	//Getters / Setters
	
	/**
	 * GameState is set by Controller or logic classes.
	 * @return the current game state
	 */
	public GameState getCurGameState(){
		return curGameState;
	}
	
	/**
	 * Get the current difficulty
	 */
	public int getCurDifficulty(){
		return curDifficulty;
	}
	
	/**
	 * Get the current categories being played
	 */
	public List<Category> getCurCategories(){
		return curCategories;
	}
	
	/**
	 * Get the OutputPanel
	 */
	public OutputPanel getOutputPanel(){
		return outputPanel;
	}
	
	/**
	 * Get the Timer
	 */
	public GUITimer getTimer(){
		return timer;
	}
	
	/**
	 * Get the LegalDragZone
	 */
	public GameBoard getLegalDragZone(){
		return gameBoard;
	}

	/**
	 * Get the current RHS tokens -- tokenBay tokens
	 */
	public List<RHSIconizedToken> getRHSIconizedTokens(){
		return GameBoard.getInstance().getTokenBayItokens();
	}

        public BracingStyle getCurBracingStyle()
        {
            return curBracingStyle;
        }

        public void setCurBracingStyle(BracingStyle style)
        {
            curBracingStyle = style;
        }
	/**
	 * Set the current difficulty
	 */
	public void setCurDifficulty(int curDifficulty){
		this.curDifficulty = curDifficulty;
	}
	
	/**
	 * Set the current categories being played.
	 */
	public void setCurCategories(List<Category> categories){
		this.curCategories = categories;
                puzzles = new HashSet<Puzzle>();
                for (Category c : categories)
                    puzzles.addAll(c.getPuzzles());
                
                puzzlesIter = puzzles.iterator();
	}
	
	/**
	 * Set the AVAILABLE categories
	 */
	public void setAvailableCategories(List<Category> categories){
		this.categories = categories;
	}
	
        public List<Category> getAvailableCategories()
        {
            if (categories == null)
                categories = APIs.getCategories();
            return categories;
        }
	/**
	 * Sets the tokenBay tokens
	 * Used by external API
	 */
	public void setTokenBayTokens(LinkedList<SourceToken> tokens){
		this.tokenBayTokens = tokens;
	}
	
	/**
	 * Sets the tokenBoard tokens
	 * Used by external API
	 */
	public void setTokenBoardTokens(LinkedList<SourceToken> tokens){
		this.tokenBoardTokens = tokens;
	}

	//--------------------------------------------------------------------------
	//PRIVATE HELPERS

        /**
	 * Activate only the buttons the user is allowed to click.
	 * Get the buttons from the controller
	 * @param buttons
	 */
	private void initButtons(ArrayList<String> buttonNames){
		LinkedList<Button> buttons = Controller.getButtons();
		
		for(Button button: buttons){
			String buttonID = button.getId();
			for(String str: buttonNames)
				if(buttonID.equals(str)){
					button.setDisable(false);
					break;
				}else
					button.setDisable(true);
		}
	}
	
	/**
	 * Uses the global activeButtons to map the game's state
	 * to a particular list of active buttons.
	 * 
	 * This is only run once per game.
	 * 
	 * Buttons are 	runButton, stopButton, pauseButton, skipButton,
	 * 				categoryButton, difficultyButton, resetBoardButton,
	 * 				logoButton
	 */
	private static void setupActiveButtonsTable(){
		
		activeButtons = new HashMap<GameState, ArrayList<String>>();
		//initialization state ("initGUI")
		ArrayList<String> initGuiBtns = new ArrayList<String>();
		initGuiBtns.add("categoryButton");
		initGuiBtns.add("logoButton");
		initGuiBtns.add("difficultyButton");
		activeButtons.put(GameState.INIT_GUI, initGuiBtns);
		
		//starting the game state ("startGame")
		ArrayList<String> startGameBtns = new ArrayList<String>();
                startGameBtns.add("runButton");
		startGameBtns.add("pauseButton");
		startGameBtns.add("skipButton");
		startGameBtns.add("categoryButton");
		startGameBtns.add("difficultyButton");
		startGameBtns.add("resetBoardButton");
		startGameBtns.add("logoButton");
		activeButtons.put(GameState.START_GAME, startGameBtns);
		
		//Pseudo game state "full left hand side" (all tokens placed on LHS)
		ArrayList<String> fullLHSbtns = new ArrayList<String>();
		fullLHSbtns.add("runButton");
		fullLHSbtns.add("pauseButton");
		fullLHSbtns.add("skipButton");
		fullLHSbtns.add("categoryButton");
		fullLHSbtns.add("difficultyButton");
		fullLHSbtns.add("resetBoardButton");
		fullLHSbtns.add("logoButton");
		activeButtons.put(GameState.FULL_LHS, fullLHSbtns);
		
		//Pseudo game state "compiling"
		ArrayList<String> compilingBtns = new ArrayList<String>();
		compilingBtns.add("stopButton");
		activeButtons.put(GameState.COMPILING, compilingBtns);
	}
        
        private static class WaitWindow extends JFrame
        {
            WaitWindow()
            {
                super("Compiling!");
                
                setSize(200, 100);
                // TODO: replace the JLabel with an animated pic here
                JPanel pn = new JPanel();
                
                pn.add(hourGlass);
                this.add(pn);
                this.setLocationRelativeTo(null);
                this.setAlwaysOnTop(true);
                this.setVisible(false);
                // do not let the user close the window
                // this will be disposed once the compilation is done
                this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                this.setResizable(false);
            }
        }
        
        private static class CompileTask implements Runnable
        {
            Output out;
            String src;
            CompileTask(String src)
            {
                this.src = src;
            }
            @Override
            public void run()
            {
                out = edu.umb.cs.api.APIs.compile(src,
                                                  currentSource.getOrinalSource().getClassName());
                
                synchronized(COMPILE_LOCK)
                {
                    COMPILE_LOCK.notifyAll();
                }
            }   
        }
 
        private static final JLabel hourGlass = getHourGlass();
        private static final Object COMPILE_LOCK = 0;
        private static final CompileTask compileTask = new CompileTask(null);
        private static final String WAIT_ICON_PATH = "/images/ui/hourglass.gif";
        private static JLabel getHourGlass()
        {
            try
            {
                return new JLabel(new ImageIcon(GUI.class.getResource(WAIT_ICON_PATH)));
            }
            catch (Exception ex)
            {
                System.out.println("Image not found!");
                ex.printStackTrace();
                return new JLabel("PLEASE WAIT"); // use text instead
            }
        }
}