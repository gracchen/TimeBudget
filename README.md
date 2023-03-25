# Time Manager - schedules your week
A desktop app that takes different parameters for classes, sleep, and etc. that may or may not depend on the day of the week as well as a list of schoolwork entries and schedules them for you.

## To run:
System requisites: At least JRE version 9
Download prototype.jar and run it.

## Latest updates:
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
