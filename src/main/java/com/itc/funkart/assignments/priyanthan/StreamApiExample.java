import java.util.*;
import java.util.stream.Collectors;

class Employee {

    String name;
    String department;
    double salary;

    Employee(String name, String department, double salary) {
        this.name = name;
        this.department = department;
        this.salary = salary;
    }
}

public class HighestSalaryByDepartment {

    public static void main(String[] args) {

        List<Employee> employees = List.of(
                new Employee("Abbas", "IT", 60000),
                new Employee("Anjali", "IT", 72000),
                new Employee("Priyanthan", "Sales", 70000),
                new Employee("Ravi", "Sales", 65000),
                new Employee("Vijay", "HR", 65000)
        );

        Map<String, Optional<Employee>> highestPaidByDept =
                employees.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.department,
                                Collectors.maxBy(Comparator.comparingDouble(e -> e.salary))
                        ));

        highestPaidByDept.forEach((dept, emp) ->
                emp.ifPresent(e ->
                        System.out.println(dept + " - " + e.name + " - " + e.salary)
                )
        );
    }
}