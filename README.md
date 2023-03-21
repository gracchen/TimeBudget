# Time Manager - schedules your week
A desktop app that takes different parameters for classes, sleep, and etc. that may or may not depend on the day of the week as well as a list of schoolwork entries and schedules them for you.

## To run:
System requisites: At least JRE version 9
Download prototype.jar and run it.

## Latest updates:
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
