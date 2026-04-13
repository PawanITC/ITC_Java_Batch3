package com.itc.funkart.product_service.assignment;

import java.util.List;
import java.util.Map;

public class EmployeeTestRunner {
    public static void main(String[] args) {
        List<Employee> employees = List.of(
                new Employee(1, "John", 5000, "IT"),
                new Employee(2, "Mary", 7000, "IT"),
                new Employee(3, "Sam", 4000, "HR"),
                new Employee(4, "David", 6000, "HR"),
                new Employee(5, "Emma", 8000, "Finance"),
                new Employee(6, "Olivia", 5500, "Finance"),
                new Employee(7, "Liam", 7200, "IT"),
                new Employee(8, "Sophia", 4500, "HR"),
                new Employee(9, "Noah", 6700, "Marketing"),
                new Employee(10, "Ava", 7500, "Marketing")
        );

        EmployeeService employeeService=new EmployeeService();
        Map<String, Employee> highestSalaryByDept=employeeService.getHighestSalary(employees);

        highestSalaryByDept.forEach((dept, emp) ->
                System.out.println(dept + " -> " + emp.getName() + " : " + emp.getSalary()));
    }
}
