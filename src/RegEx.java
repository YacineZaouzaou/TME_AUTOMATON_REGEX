import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;




public class RegEx {
	//COLOR STRING
	final static String RED = (char) 27 + "[31;1m";
	final static String REDBG = (char) 27 + "[41;1m";
	final static String RESETCOLOR = (char) 27 + "[0m";




	// MACROS
	static final int CONCAT = 0xC04CA7;
	static final int ETOILE = 0xE7011E;
	static final int ALTERN = 0xA17E54;
	static final int PROTECTION = 0xBADDAD;
	static final int PLUS = 0x2b;

	static final int PARENTHESEOUVRANT = 0x16641664;
	static final int PARENTHESEFERMANT = 0x51515151;
	static final int BACKSLACH = 0xE1515151;
	static final int DOT = 0xD0AC37;
	static final int TRICKDOT = 0x54321;

	static final int MAX_STATE = 100;
	static final int MAX_TRANS = 100;

	// REGEX
	private static String regEx;
	private static String filePath;
	private static boolean color;
	private static int automaton[][][] = new int[MAX_STATE][256][MAX_TRANS];
	private static int states[][] = new int[MAX_STATE][2]; // 0 for initial , 1 for final
	private static int epsilons[][] = new int[MAX_STATE][MAX_TRANS];
	private static int cpt = 0;
	private static Stack<int[]> stack = new Stack<>();

	// CONSTRUCTOR
	private RegEx() {
	}

	
	
	
	
	
	/**
	 * 
	 * 
	 * 
	 * 
	 * @param regex			the pattern searched in the text
	 * @param filePath		the path to file containing the text
	 * @return				a list of arrays with the following pattern : 
	 * 							for each match : an array [ numberOfLine , PositionOfFirstcharacter , PositionOfLastcharacter ]
	 */
	public static  List<int []> find (String regex , String filepath) throws Exception {
		regEx = regex;
		System.out.print("lookin for  : "+regex);
		filePath = filepath;
		System.out.println(", in file : "+filePath);
		color = false;
		return launchProcess();
	}
	
	
	public static  List<int []> findColor (String regex , String filepath) throws Exception {
		regEx = regex;
		System.out.print("lookin for  : "+regex);
		filePath = filepath;
		System.out.println(", in file : "+filePath);
		color = true;
		return launchProcess();
	}
	
	private static List<int []> launchProcess () throws Exception{
		init_tabs();
		RegExTree tree = parse();
		toAutomaton(tree);
		determinisation();
		List<int []> positions  = match_count_advance("sargon.txt");
		System.out.println(positions.size());
		return positions;
	}
	


	/**
	 * print function section
	 */
	
	
	private static void printAuto() {
		System.out.print("\t");
		for (int j = 97; j < (97 + 27); j++) {
			System.out.print( (char)j+"\t");
		}
		System.out.println();
		for (int i = 0; i < cpt; i++) {
			System.out.print("("+i+")\t");
			for (int j = 97; j < (97 + 27); j++) {

				print_tab(automaton[i][j]);

			}
			System.out.println();
		}
		System.out.println("\nprinting epsilon");
		for (int i = 0; i < cpt; i++) {
			print_tab(epsilons[i]);
			System.out.println();
		}

		System.out.println("\nstarting states : ");
		for (int i = 0; i < states.length; i++) {
			if (states[i][0] == 1)
				System.out.print(i + " ");
		}
		System.out.println("\nending states");
		for (int i = 0; i < states.length; i++) {
			if (states[i][1] == 1)
				System.out.print(i + " ");
		}
		System.out.print("\t");
	}

	private static void print_tab(int[] tab) {
		System.out.print("{");
		for (int i = 0; i < tab.length; i++) {
			if (tab[i] != -1)
				System.out.print(tab[i] + " ");
		}
		System.out.print("}\t");
	}
	

	private static void printPositions (List<int []> list) {
		for (int [] word : list) {
			System.out.println("at line : "+word[0]+" starts at : "+word[1]+" ends at "+word[2]);
		}
	}
	
	private static void printlist (List <Integer> list) {
		for (Integer integer : list) {
			System.out.print(integer+" ");
		}
	}

	
	
	/**
	 * print function section end
	 */

	
	/**
	 * automaton util functions
	 */
	
	
	private static void init_tabs() {
		// init automaton & states
		for (int i = 0; i < automaton.length; i++) {
			for (int j = 0; j < automaton[i].length; j++) {
				for (int k = 0; k < automaton[i][j].length; k++) {
					automaton[i][j][k] = -1;
				}
			}
			states[i][0] = -1;
			states[i][1] = -1;
		}

		// init epsilon
		for (int i = 0; i < epsilons.length; i++) {
			for (int j = 0; j < epsilons[i].length; j++) {
				epsilons[i][j] = -1;
			}
		}

	}

	private static void init_tabs(int[][][] automaton, int[][] epsilons, int[][] states) {
		// init automaton & states
		for (int i = 0; i < automaton.length; i++) {
			for (int j = 0; j < automaton[i].length; j++) {
				for (int k = 0; k < automaton[i][j].length; k++) {
					automaton[i][j][k] = -1;
				}
			}
			states[i][0] = -1;
			states[i][1] = -1;
		}

		// init epsilon
		for (int i = 0; i < epsilons.length; i++) {
			for (int j = 0; j < epsilons[i].length; j++) {
				epsilons[i][j] = -1;
			}
		}

	}
	
	
	
	

	private static void toAutomaton(RegExTree tree) throws Exception {
		if (tree.subTrees.isEmpty()) {
			if (!isLetter(tree.root)) {
				throw new Exception();
			}
			if(tree.root == DOT){
				int[] s = new int[2];
				s[0] = cpt++;
				s[1] = cpt++;
				addDot(s[0], s[1]);
				stack.push(s);
			}else {

				int[] s = new int[2];
				s[0] = cpt++;
				s[1] = cpt++;
				valueTrans(tree.root, s[0], s[1]);
				stack.push(s);
			}
		} else {
			for (RegExTree trees : tree.subTrees) {
				toAutomaton(trees);
			}
			switch (tree.root) {
			case RegEx.BACKSLACH: {
				break;
			}
			case RegEx.ETOILE: {
				int[] lastValue = stack.pop();
				clos(lastValue[0], lastValue[1]);
				break;
			}
			case RegEx.PLUS : {
				int[] lastValue = stack.pop();
				plus(lastValue[0], lastValue[1]);
				break;
			}
			case RegEx.CONCAT: {
				int[] lastValue2 = stack.pop();
				int[] lastValue1 = stack.pop();
				concat(lastValue1[0], lastValue1[1], lastValue2[0], lastValue2[1]);
				break;
			}
			case RegEx.ALTERN: {
				int[] lastValue2 = stack.pop();
				int[] lastValue1 = stack.pop();
				union(lastValue1[0], lastValue1[1], lastValue2[0], lastValue2[1]);
				break;
			}
			default:
				break;
			}
		}

	}

	private static boolean isLetter(int root) {
		if (root == RegEx.ALTERN || root == RegEx.CONCAT || root == RegEx.ETOILE || root == RegEx.BACKSLACH)
			return false;
		return true;
	}

	private static void nothingTrans(int s1, int s2) {

	}

	private static void epsilonTrans(int s1, int s2) {
		addEpsilon(s1, s2);
		states[s1][0] = 1;
		states[s1][1] = 1;
	}

	private static void valueTrans(int value, int s1, int s2) {
		int i = 0;
		while (automaton[s1][value][i] != -1)
			i++;
		automaton[s1][value][i] = s2;
		states[s1][0] = 1;
		states[s2][1] = 1;
	}

	// très lourde pour les grand automates ::: technique à revoir si on veut faire
	// des grand automates
	private static void addEpsilon(int s1, int s2) {
		int i = 0;
		while (epsilons[s1][i] != -1)
			i++;
		epsilons[s1][i] = s2;
	}

	private static void addDot (int s1 , int s2) {
		int i = 0;
		for (int j = 0 ; j < 128 ; j ++) {
			i = 0;
			while (automaton[s1][j][i] != -1)
				i++;
			automaton[s1][j][i] = s2;
		}
		states[s1][0] = 1;
		states[s2][1] = 1;
	}


	private static void union(int s1_1, int s1_2, int s2_1, int s2_2) {
		int s1 = cpt++;
		int s2 = cpt++;
		states[s1][0] = 1;
		states[s1][1] = 0;
		states[s2][0] = 0;
		states[s2][1] = 1;
		states[s1_1][0] = 0;
		states[s2_1][0] = 0;
		states[s1_2][1] = 0;
		states[s2_2][1] = 0;
		addEpsilon(s1, s1_1);
		addEpsilon(s1, s2_1);
		addEpsilon(s1_2, s2);
		addEpsilon(s2_2, s2);
		stack.push(new int[] { s1, s2 });
	}

	private static void concat(int s1_1, int s1_2, int s2_1, int s2_2) {
		states[s1_2][1] = 0;
		states[s2_1][0] = 0;
		addEpsilon(s1_2, s2_1);
		stack.push(new int[] { s1_1, s2_2 });
	}

	private static void clos(int s1_1, int s1_2) {
		int s1 = cpt++;
		int s2 = cpt++;
		states[s1][0] = 1;
		states[s1][1] = 0;
		states[s2][1] = 1;
		states[s2][0] = 0;
		states[s1_1][0] = 0;
		states[s1_2][1] = 0;
		addEpsilon(s1_2, s1_1);
		addEpsilon(s1, s2);
		addEpsilon(s1, s1_1);
		addEpsilon(s1_2, s2);
		stack.push(new int[] { s1, s2 });
	}

	private static void plus (int s1_1, int s1_2) {
		int s1 = cpt++;
		int s2 = cpt++;
		states[s1][0] = 1;
		states[s1][1] = 0;
		states[s2][1] = 1;
		states[s2][0] = 0;
		states[s1_1][0] = 0;
		states[s1_2][1] = 0;
		addEpsilon(s1_2, s1_1);
		addEpsilon(s1, s1_1);
		addEpsilon(s1_2, s2);
		stack.push(new int[] { s1, s2 });
	}
	
	
	/**
	 *	automaton util functions end  
	 */
	


	private static boolean match(String fileName) throws Exception {
		BufferedReader bf = new BufferedReader(new FileReader(new File(fileName)));
		String line;
		int pos = 0;
		while ((line = bf.readLine()) != null) {
			//			line = line.toLowerCase();
			for (int i = 0; i < line.length(); i++) {
				// ONE STATE REACHED AT TIME
				int c = (int) line.charAt(i);
				if (c >= 256 || c < 0) {
					continue;
				}
				if (automaton[pos][c][0] == -1) {
					pos = 0;
					continue;
				}
				pos = automaton[pos][c][0];

				if (states[pos][1] == 1)
					return true;
			}
		}
		return false;
	}

	private static int match_count(String fileName) throws Exception {
		BufferedReader bf = new BufferedReader(new FileReader(new File(fileName)));
		String line;
		int nbMatch = 0;
		int l = 0;
		int pos = 0;
		while ((line = bf.readLine()) != null) {
			l++;
			//			line = line.toLowerCase();
			for (int i = 0; i < line.length(); i++) {
				// ONE STATE REACHED AT TIME
				int c = (int)line.charAt(i);
				if (c >= 256 || c < 0) {
					continue;
				}
				if (automaton[pos][c][0] == -1) {
					pos = 0;
					continue;
				}
				pos = automaton[pos][c][0];

				if (states[pos][1] == 1) {
					System.out.println(l);
					pos = 0;
					nbMatch++;
				}
			}
		}
		return nbMatch;
	}


	private static List<int []> match_count_advance(String fileName) throws Exception {
		List<int []> positions = new ArrayList<>();
		BufferedReader bf = new BufferedReader(new FileReader(new File(fileName)));
		String line;
		int nbMatch = 0;
		int l = 0;
		int pos = 0;
		int start = -1;
		boolean write = false;
		StringBuilder printLine = null;
		StringBuilder printWord = null;
		while ((line = bf.readLine()) != null) {
			write = false;
			start = - 1;
			pos = 0;
			l++;
			printLine = new StringBuilder();
			printWord = new StringBuilder();
			for (int i = 0; i < line.length(); i++) {
				
				// empty word treatment ex : (ANNYTHING)* -- matched words are : ANNYTHING and empty word
				if (states[pos][1] == 1 && imlast(line , i , pos)) {
					write = true;
					nbMatch++;
					positions.add(new int [] {l , start , i});
				}
				// ONE STATE REACHED AT TIME
				int c = (int)line.charAt(i);
				
				// deleting all non ASCII character
				if (c >= 256 || c < 0) {
					if (printWord.length() > 0) {
						printLine.append(printWord.charAt(0));
						printWord = new StringBuilder();
					}else {
						printLine.append((char)c);
					}
					continue;
				}
				if (automaton[pos][c][0] == -1) {
					if (pos > 0)
						i = start;
					start = -1;
					pos = 0;
					if (printWord.length() > 0) {
						printLine.append(printWord.charAt(0));
						printWord = new StringBuilder();
					}else {
						printLine.append((char)c);
					}
					continue;
				}
				pos = automaton[pos][c][0];
				if (start == -1)
					start = i;
				printWord.append((char)c);

				if (states[pos][1] == 1 && imlast(line , i , pos)) {
					write = true;
					if (color) {
						printLine.append(RED+printWord.toString()+RESETCOLOR);
					}else {
						printLine.append(printWord.toString());
					}
					printWord = new StringBuilder();
					pos = 0;
					nbMatch++;
					positions.add(new int [] {l , start , i});
					start = -1;
				}
				if (i == line.length() - 1)
					printLine.append(printWord);
			}
			if (write)
				System.out.println(printLine.toString());
		}
		return positions;
	}

	private static boolean imlast (String line , int index , int pos) {
		if (index == line.length() - 1)
			return true;
		index++;
		if (line.charAt(index) >= 256 || line.charAt(index) < 0)
			return true;
		pos = automaton[pos][line.charAt(index)][0];
		if (pos == -1) return true;

		return states[pos][1] != 1;

	}



	private static void determinisation() {
		int[][][] new_automaton = new int[MAX_STATE][256][MAX_TRANS];
		int[][] new_eplisons = new int[MAX_STATE][MAX_TRANS];
		int[][] new_states = new int[MAX_STATE][2];
		init_tabs(new_automaton, new_eplisons, new_states);
		int local_cpt = 0;
		List<Integer>[] generatedStates = getStartingDeterminisation();
		// getting number of generated state
		while (generatedStates[local_cpt] != null) {
			local_cpt++;
		}
		ArrayList<Integer> allTrans = getTransitions();
		for (int i = 0; i < local_cpt; i++) {
			int count = 0;
			for (int t : allTrans) {
				count = 0;
				for (int s : generatedStates[i]) {

					for (int dest : automaton[s][t]) {
						if (dest != -1) {
							new_automaton[i][t][count++] = dest;
						}
						// add epsilons
						for (int k = 0; k < count; k++) {
							for (int esp : epsilons[new_automaton[i][t][k]]) {
								if (esp != -1) {
									if (isnotIn(new_automaton[i][t], count, esp)) {
										new_automaton[i][t][count++] = esp;
									}
								}
							}
						}
					}
				}
				if (is_new_state(generatedStates, new_automaton[i][t], local_cpt,count)) {
					ArrayList<Integer> new_ = computeState(new_automaton[i][t]);
					generatedStates[local_cpt++] = new_;
				}
			}
		}
		for (int i = 0; i < local_cpt; i++) {
			for (int state : generatedStates[i]) {
				if (states[state][0] == 1) {
					new_states[i][0] = 1;
				}
				if (states[state][1] == 1) {
					new_states[i][1] = 1;
				}
			}
		}
		cpt = local_cpt;
		automaton = reLabel(new_automaton, generatedStates);
		states = new_states;
	}

	private static ArrayList<Integer> computeState (int [] tab) {
		ArrayList<Integer> array = new ArrayList<Integer>();
		for (int i : tab) {
			if (i == -1)
				break;
			array.add(i);
		}
		return array;
	}

	private static int[][][] reLabel(int[][][] auto, List<Integer>[] labels) {
		int[][][] returnValue = new int[auto.length][auto[0].length][auto[0][0].length];
		for (int i = 0; i < returnValue.length; i++) {
			for (int j = 0; j < returnValue[0].length; j++) {
				if (auto[i][j][0] == -1) {
					for (int k = 0; k < returnValue[i][j].length; k++) {
						returnValue[i][j][k] = -1;
					}
				} else {
					int value = index(labels, auto[i][j]);
					returnValue[i][j][0] = index(labels, auto[i][j]);
					for (int k = 1; k < returnValue[i][j].length; k++) {
						returnValue[i][j][k] = -1;
					}
				}
			}
		}
		return returnValue;
	}

	private static int index(List<Integer>[] labels, int[] tab) {
		// we don't need to go until the end
		for (int i = 0; i < labels.length; i++) {
			if (equalTab(labels[i], tab))
				return i;
		}
		return -1;
	}

	/**
	 * not tested
	 * 
	 * @param label
	 * @param tab
	 * @return
	 */
	private static boolean equalTab(List<Integer> label, int[] tab) {
		if (label == null) return false;
		for (int i : tab) {
			if (i == -1)
				break;
			if (!label.contains(i)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isnotIn(int[] tab, int size, int value) {
		for (int i = 0; i < size; i++) {
			if (tab[i] == value) {
				return false;
			}
		}
		return true;
	}

	private static void printNewState(List<Integer>[] list, int size) {
		for (int i = 0; i < size; i++) {
			System.err.print(i + " -> ");
			for (int j : list[i])
				System.err.print(j + " ");
			System.err.println();
		}
	}



	private static boolean isIn(int [] tab , int value) {
		boolean ret = false;
		for (int i : tab) {
			if (i == -1)
				break;
			ret = true;
			if (i != value)
				return false;

		}
		return ret;
	}


	/**
	 * TODEL 
	 */
	private static int computeSize (int [] tab) {
		int i = 0;
		while (tab[i] != -1)
			i++;
		return i;
	}


	private static boolean is_new_state(List<Integer>[] allStates, int[] foundedState, int sizeAll, int sizeFounded) {
		sizeFounded = computeSize(foundedState);
		boolean is_new = false;
		for (int i = 0; i < sizeAll; i++) {
			if (isEqualTo(allStates[i], foundedState, sizeFounded)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * computes if ensB is included in ensA
	 * 
	 * i use List for the first argument because of my previous choices
	 * 
	 * 
	 * @param ensA
	 * @param ensB
	 * @param sizeb added because of my previous choices
	 * @return
	 */
	private static boolean isIncluded(List<Integer> ensA, int[] ensB, int sizeB) {
		for (int i = 0; i < sizeB; i++) {
			if (!ensA.contains(ensB[i]))
				return false;
		}
		return true;
	}

	private static boolean isEqualTo (List<Integer> ensA , int [] ensB , int sizeB) {
		if (ensA.size() != sizeB)
			return false;
		for (int i = 0 ; i < sizeB ; i ++) {
			if (! ensA.contains(ensB[i]))
				return false;
		}
		return ensA.size() == sizeB;
	}

	private static void test_isEqualTo () {

		int [] ensB = new int [] {1,2,3,4, 5};
		ArrayList<Integer> ensA = new ArrayList<>();
		ensA.add(2);
		ensA.add(3);
		ensA.add(1);
		ensA.add(4);
		System.out.println(isEqualTo(ensA, ensB, 5));
	}


	/**
	 * get all transition labels existing in our automaton for Ex : for ac*|b it
	 * returns a list [ 97 , 98 , 99 ] ascci equavalent of [ a , b , c ]
	 */
	private static ArrayList<Integer> getTransitions() {
		Set<Integer> transitions = new HashSet<Integer>();
		for (int i = 0; i < cpt; i++) {
			for (int j = 0; j < 256; j++) {
				if (automaton[i][j][0] != -1) {
					transitions.add(j);
				}
			}
		}
		return toArrayList(transitions);
	}

	private static ArrayList<Integer> toArrayList (Set<Integer> set ) {
		ArrayList<Integer> arraylist = new ArrayList<>();
		for (Integer integer : set) {
			arraylist.add(integer);
		}
		return arraylist;
	}


	/**
	 * getting all starting states it looks for all starting states and all states
	 * accessible with an epsilon transition. every starting state from the original
	 * automaton is a starting state
	 * 
	 * @return
	 */
	private static List<Integer>[] getStartingDeterminisation() {
		List<Integer>[] returnValue = new ArrayList[MAX_STATE];
		int ac = 0;
		//		List<Integer> local = null;
		int[] local = null;
		int cp = 0;
		for (int i = 0; i < states.length; i++) {
			if (states[i][0] == 1 && isnotIn(returnValue, ac, i)) {
				local = new int[MAX_STATE];
				cp = 0;
				local[cp++] = i;
				for (int j = 0; j < cp; j++) {
					for (int k = 0; k < MAX_STATE; k++) {
						if (epsilons[local[j]][k] == -1) {
							break;
						} else if (isnotIn(local, cp, epsilons[local[j]][k])) {
							local[cp++] = epsilons[local[j]][k];
						}
					}
				}
				returnValue[ac++] = tabToList(local, cp);
			}

		}
		return returnValue;
	}

	private static ArrayList<Integer> tabToList(int[] tab, int size) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			list.add(tab[i]);
		}
		return list;
	}

	/**
	 * 
	 * false if state is in list true if state is not in list
	 * 
	 * @param list  of states composed by regrouping all states accessible by
	 *              epsilon transition
	 * @param size  size of the list (the list is a fixed size array, size gives the
	 *              last full case
	 * @param state
	 * @return
	 */
	private static boolean isnotIn(List<Integer>[] list, int size, int state) {
		for (List<Integer> l : list) {
			if (l == null)
				return true;
			if (l.contains(state))
				return false;
		}
		return true;
	}

	
	
	
	
	
	
	
	
	
	
	/**
	 * original code downloaded.
	 */

	// FROM REGEX TO SYNTAX TREE
	private static RegExTree parse() throws Exception {
		// BEGIN DEBUG: set conditionnal to true for debug example
		if (false)
			throw new Exception();
		RegExTree example = exampleAhoUllman();
		if (false)
			return example;
		// END DEBUG

		ArrayList<RegExTree> result = new ArrayList<RegExTree>();
		for (int i = 0; i < regEx.length(); i++)
			result.add(new RegExTree(charToRoot(regEx.charAt(i)), new ArrayList<RegExTree>()));

		
		
		return parse(result);
	}

	private static int charToRoot(char c) {
		if (c == '.')
			return (int) '.';
		if (c == '*')
			return ETOILE;
		if (c == '+')
			return PLUS;
		if (c == '|')
			return ALTERN;
		if (c == '(')
			return PARENTHESEOUVRANT;
		if (c == ')')
			return PARENTHESEFERMANT;
		if (c == '\\')
			return (int) '\\';
		return (int) c;
	}

	private static RegExTree parse(ArrayList<RegExTree> result) throws Exception {
		while (containParenthese(result))
			result = processParenthese(result);
		while (containBackslach(result))
			result = processBackslach(result);
		while (containDot(result))
			result = processDot(result);
		while (containEtoile(result))
			result = processEtoile(result);
		while (containPlus(result))
			result = processPlus(result);
		while (containConcat(result))
			result = processConcat(result);
		while (containAltern(result))
			result = processAltern(result);

		if (result.size() > 1)
			throw new Exception();

		return removeProtection(result.get(0));
	}

	private static boolean containParenthese(ArrayList<RegExTree> trees) {
		for (RegExTree t : trees)
			if (t.root == PARENTHESEFERMANT || t.root == PARENTHESEOUVRANT)
				return true;
		return false;
	}

	
	/**
	 * section i added
	 */
	
	
	private static boolean containBackslach (ArrayList<RegExTree> trees) {
		for (RegExTree t : trees) {
			if ( t.root == '\\' ) 
				return true;
			}
		return false;
	}
	
	private static boolean containDot (ArrayList<RegExTree> trees) {
		for (RegExTree t : trees)
			if (t.root == (int) '.')
				return true;
		return false;
	}
	
	private static ArrayList<RegExTree> processDot (ArrayList<RegExTree> trees) throws Exception {
		ArrayList<RegExTree> result = new ArrayList<RegExTree>();
		boolean found = false;
		for (RegExTree t : trees) {
			if (!found && t.root == (int)'.' && t.subTrees.isEmpty()) {
				found = true;
				ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
				result.add(new RegExTree(DOT, subTrees));
			} else {
				result.add(t);
			}
		}
		return result;
	}

	private static ArrayList<RegExTree> processBackslach (ArrayList<RegExTree> trees) throws Exception {
		ArrayList<RegExTree> result = new ArrayList<RegExTree>();
		boolean found = false;
		boolean dontAdd = true;
		for (RegExTree t : trees) {
			if ( t.root == (int) '\\' && t.subTrees.isEmpty()) {
				if (trees.size()-1 == trees.indexOf(t))
					throw new Exception();
				found = true;
				dontAdd = false;
				RegExTree last = trees.get(trees.indexOf(t)+1);
				ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
				subTrees.add(last);
				result.add(new RegExTree(BACKSLACH, subTrees));
			} else {
				if (dontAdd)
					result.add(t);
				dontAdd = true;
			}
			
			
		}
		return result;
	}
	
	
	
	/**
	 *	section i added end 
	 */


	private static ArrayList<RegExTree> processParenthese(ArrayList<RegExTree> trees) throws Exception {
		ArrayList<RegExTree> result = new ArrayList<RegExTree>();
		boolean found = false;
		for (RegExTree t : trees) {
			if (!found && t.root == PARENTHESEFERMANT) {
				boolean done = false;
				ArrayList<RegExTree> content = new ArrayList<RegExTree>();
				while (!done && !result.isEmpty())
					if (result.get(result.size() - 1).root == PARENTHESEOUVRANT) {
						done = true;
						result.remove(result.size() - 1);
					} else
						content.add(0, result.remove(result.size() - 1));
				if (!done)
					throw new Exception();
				found = true;
				ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
				subTrees.add(parse(content));
				result.add(new RegExTree(PROTECTION, subTrees));
			} else {
				result.add(t);
			}
		}
		if (!found)
			throw new Exception();
		return result;
	}

	private static boolean containEtoile(ArrayList<RegExTree> trees) {
		for (RegExTree t : trees)
			if (t.root == ETOILE && t.subTrees.isEmpty())
				return true;
		return false;
	}

	private static ArrayList<RegExTree> processEtoile(ArrayList<RegExTree> trees) throws Exception {
		ArrayList<RegExTree> result = new ArrayList<RegExTree>();
		boolean found = false;
		for (RegExTree t : trees) {
			if (!found && t.root == ETOILE && t.subTrees.isEmpty()) {
				if (result.isEmpty())
					throw new Exception();
				found = true;
				RegExTree last = result.remove(result.size() - 1);
				ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
				subTrees.add(last);
				result.add(new RegExTree(ETOILE, subTrees));
			} else {
				result.add(t);
			}
		}
		return result;
	}


	private static boolean containConcat(ArrayList<RegExTree> trees) {
		boolean firstFound = false;
		for (RegExTree t : trees) {
			if (!firstFound && t.root != ALTERN) {
				firstFound = true;
				continue;
			}
			if (firstFound)
				if (t.root != ALTERN)
					return true;
				else
					firstFound = false;
		}
		return false;
	}

	private static ArrayList<RegExTree> processConcat(ArrayList<RegExTree> trees) throws Exception {
		ArrayList<RegExTree> result = new ArrayList<RegExTree>();
		boolean found = false;
		boolean firstFound = false;
		for (RegExTree t : trees) {
			if (!found && !firstFound && t.root != ALTERN) {
				firstFound = true;
				result.add(t);
				continue;
			}
			if (!found && firstFound && t.root == ALTERN) {
				firstFound = false;
				result.add(t);
				continue;
			}
			if (!found && firstFound && t.root != ALTERN) {
				found = true;
				RegExTree last = result.remove(result.size() - 1);
				ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
				subTrees.add(last);
				subTrees.add(t);
				result.add(new RegExTree(CONCAT, subTrees));
			} else {
				result.add(t);
			}
		}
		return result;
	}

	private static boolean containAltern(ArrayList<RegExTree> trees) {
		for (RegExTree t : trees)
			if (t.root == ALTERN && t.subTrees.isEmpty())
				return true;
		return false;
	}

	private static boolean containPlus(ArrayList<RegExTree> trees) throws Exception {
		for (RegExTree t : trees)
			if (t.root == PLUS && t.subTrees.isEmpty())
				return true;
		return false;
	}

	private static ArrayList<RegExTree> processPlus(ArrayList<RegExTree> trees) throws Exception {
		ArrayList<RegExTree> result = new ArrayList<RegExTree>();
		boolean found = false;
		for (RegExTree t : trees) {
			if (!found && t.root == PLUS && t.subTrees.isEmpty()) {
				if (result.isEmpty())
					throw new Exception();
				found = true;
				RegExTree last = result.remove(result.size() - 1);
				ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
				subTrees.add(last);
				result.add(new RegExTree(PLUS, subTrees));
			} else {
				result.add(t);
			}
		}
		return result;
	}



	private static ArrayList<RegExTree> processAltern(ArrayList<RegExTree> trees) throws Exception {
		ArrayList<RegExTree> result = new ArrayList<RegExTree>();
		boolean found = false;
		RegExTree gauche = null;
		boolean done = false;
		for (RegExTree t : trees) {
			if (!found && t.root == ALTERN && t.subTrees.isEmpty()) {
				if (result.isEmpty())
					throw new Exception();
				found = true;
				gauche = result.remove(result.size() - 1);
				continue;
			}
			if (found && !done) {
				if (gauche == null)
					throw new Exception();
				done = true;
				ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
				subTrees.add(gauche);
				subTrees.add(t);
				result.add(new RegExTree(ALTERN, subTrees));
			} else {
				result.add(t);
			}
		}
		return result;
	}

	private static RegExTree removeProtection(RegExTree tree) throws Exception {
		if (tree.root == PROTECTION && tree.subTrees.size() != 1)
			throw new Exception();
		if (tree.subTrees.isEmpty())
			return tree;
		if (tree.root == PROTECTION)
			return removeProtection(tree.subTrees.get(0));

		ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
		for (RegExTree t : tree.subTrees)
			subTrees.add(removeProtection(t));
		return new RegExTree(tree.root, subTrees);
	}

	// EXAMPLE
	// --> RegEx from Aho-Ullman book Chap.10 Example 10.25
	private static RegExTree exampleAhoUllman() {
		RegExTree a = new RegExTree((int) 'a', new ArrayList<RegExTree>());
		RegExTree b = new RegExTree((int) 'b', new ArrayList<RegExTree>());
		RegExTree c = new RegExTree((int) 'c', new ArrayList<RegExTree>());
		ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
		subTrees.add(c);
		RegExTree cEtoile = new RegExTree(ETOILE, subTrees);
		subTrees = new ArrayList<RegExTree>();
		subTrees.add(b);
		subTrees.add(cEtoile);
		RegExTree dotBCEtoile = new RegExTree(CONCAT, subTrees);
		subTrees = new ArrayList<RegExTree>();
		subTrees.add(a);
		subTrees.add(dotBCEtoile);
		return new RegExTree(ALTERN, subTrees);
	}
}

//UTILITARY CLASS
class RegExTree {
	protected int root;
	protected ArrayList<RegExTree> subTrees;

	public RegExTree(int root, ArrayList<RegExTree> subTrees) {
		this.root = root;
		this.subTrees = subTrees;
	}

	// FROM TREE TO PARENTHESIS
	public String toString() {
		if (subTrees.isEmpty())
			return rootToString();
		String result = rootToString() + "(" + subTrees.get(0).toString();
		for (int i = 1; i < subTrees.size(); i++)
			result += "," + subTrees.get(i).toString();
		return result + ")";
	}

	private String rootToString() {
		if (root == RegEx.CONCAT)
			return ".";
		if (root == RegEx.ETOILE)
			return "*";
		if (root == RegEx.ALTERN)
			return "|";
		if (root == RegEx.DOT)
			return ".";
		if (root == RegEx.BACKSLACH)
			return "\\";
		return Character.toString((char) root);
	}
}
