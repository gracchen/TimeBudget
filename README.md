# Time Manager - schedules your week
A desktop app that takes different parameters for classes, sleep, and etc. that may or may not depend on the day of the week as well as a list of schoolwork entries and schedules them for you.

## To run:
System requisites: At least JRE version 9
Download prototype.jar and run it.

## Latest updates:
#### 3/26/2023: sudden burst of unforseen complexities, getting intimidated
added: 
- splitting algorithm (introduces complexities of marking which partitions as complete, where to store dupes and how to keep track which is parent/child of which, etc)
- rudimentary gui for displaying assigned tasks as dummy checklists & tabs
- in middle of figuring how to save what before exiting, see below points:

complexities/questions/toDos:
- once user checks task, when to commit "done"? before exiting? straight away? because hard to uncommit.
     - if commit before exit, save index of work entries to delete at end? need custom exiting method call...more complexity. what if app not exit properly/crashed?
     - if auto detects unchecked event past deadline, what to do? auto set as done?
     - if user checks a spilt component of parent, have to merge back together splits, sum up unchecked durations, store back in 1 original entry? 
     - done: purely append (cheaper than reading globally done list and copying)?
     - data structure to represent stars????  display table of done? how far back?? $$$$$$$costly

- future million features:
     - implement functioning tasks tab: user add delete work entries, settings, more insane complexity
     - after that is proven correct, how to make every write and read a binary file not txt??? 
     - settings: fixed number/type of constants/params? or ability to add like tasks? 
     - don't even get me started on online database features or what if task taking way longer than expected how to reflect on app??

#### 3/25/2023:
rudimentary scheduling algorithm fed sorted work entries
- compares possible ideal days to assign (today til day before deadline)
- if all days not enough hrs left to do it, force assign to deadline
- if at least one day has enough hours, find best day (takes the least amt of time away from ideal leet+play time for that type of day (weekend/day))
- ISSUE: won't break up tasks in the "forced" case, leading to negative hours left (chose the least negative, but still negative) even if possible to distribute between previous days without negative budgets.

data structs hard to keep track, so here's run-down:
- week: array of 7 LocalDates from today -> week from now
- budget: array of 7 doubles (hrs) for global budget during classes, budget[0] = all monday's, budget[1] = tuesday's
- budgetB: array of 7 doubles (hrs) for global budget during breaks
- hrsLeft: array of 7 doubles (hrs) calculated from assigned work + budget/budgetB
- work: array of Entry objects read from school.txt
- assign: array of 7 lists of indexes, where assign[n] houses a list of work indexes assigned to the day that's n days from today.

functions too:
- int Ideal(LocalDate x) gives ideal remaining hrs for leetcode & play for LocalDate x (is x weekday or weekend?)
- void printBudget() prints budget or budgetB for next 7 days 
- void printDates() prints entire status of week, budget/B, and assigned work for each day in assign
- private void distrAlg() recalculates hrsLeft and re-assigns work, called when toggle break button.
- int dayToIndex(LocalDate y) converts date's day of week to corresponding index for budget[] to work
- void readConstants(), readParams(), and readSchool() scans and stores txt data into useable data structures

New used features: 
- copy arraylist to arraylist: copy = new ArrayList<Double>(old)
- List of a list: List<List<Integer>> a = new ArrayList<List<Integer>>(); 
- get num days between two LocalDates: ChronoUnit.DAYS.between(a, b)

#### 3/24/2023:
<img alt="YAP" src="https://i.imgur.com/m47GwfF.png">
Reads school.txt (containing "name", difficulty/5, estimated hours to complete, deadline, and an optional "fixed" flag

Puts it into array of entry objects holding all the above

Custom comparator to sort array by #1 fixed always on top  #2 deadline #3 difficulty

Toggle break mode (i.e. during school weeks or spring break?) to adjust budget

- Reusable date formatter: formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
     - LocalDate-->str: formatter.format(date);
     - str-->LocalDate: LocalDate.parse(str, formatter);
- class SORT implements Comparator<Type> {  }
     - public int compare(Type o1, Type o2) { }  
     - returns 0 if equal, pos if o1 before o2, neg if o2 before o1.
     - For complex var comparison: o1.isEqual(o2), o1.compareTo(o2);
     - For primitive var cmp: Double.compare(o1, o2); //to sort descending: o2,o1
     - to use sort(): Collections.sort(arr, new SORT());
- str.substring(startIndex, endIndex)  vs C++:  (startIndex, length);
     - just 1 argument = startIndex --> end of str

#### 3/20/2023:
Reads params.txt (containing only daily hourly parameters + title) and subtracts from every day's 24 budget. 
Reads constants.txt (classes duration + eating/commuting, first row is Monday, last row is Sunday).
Then calculates the next 7 days' remaining budget and prints to console.

- Basic array: double consts[] = new double[7];
- With initialized value: List<Double> budget = new ArrayList<Double>(Collections.nCopies(7,24.0));
- Vectorlike: List<LocalDate> week = new LinkedList<LocalDate>();
     - to access element: week.get(index) and set()
     - to add:  .add();
- deliminating: String temp[] = str.split(",");
- to print 3/4/2000 style: DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(curr)
