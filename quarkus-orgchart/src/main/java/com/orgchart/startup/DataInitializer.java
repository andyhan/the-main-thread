package com.orgchart.startup;

import com.orgchart.entity.Employee;
import com.orgchart.repository.EmployeeRepository;

import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@Startup
@ApplicationScoped
public class DataInitializer {

    @Inject
    EmployeeRepository repository;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (repository.count() > 0) {
            return;
        }

        Employee ceo = create("Sarah", "Johnson", "CEO", "Executive", null);
        Employee cto = create("Michael", "Chen", "CTO", "Technology", ceo);
        Employee vpEng = create("Emily", "Brown", "VP Engineering", "Engineering", cto);
        Employee pm = create("Markus", "Eisele", "Secret Weapon", "Product Marketing", vpEng);

        create("Thomas", "Moore", "Engineering Manager", "Engineering", vpEng);
        create("Sarah", "Robinson", "Backend Engineer", "Engineering", vpEng);
        create("Joshua", "Clark", "Backend Engineer", "Engineering", vpEng);

        create("Amazing", "Jonathan", "Backend Engineer", "Engineering", pm);
        create("Another", "Name", "Frontend Engineer", "Engineering", pm);
        
        
    }

    private Employee create(String first,
            String last,
            String title,
            String dept,
            Employee manager) {
        Employee e = new Employee();
        e.firstName = first;
        e.lastName = last;
        e.title = title;
        e.department = dept;
        e.email = first.toLowerCase() + "." + last.toLowerCase() + "@example.com";
        e.manager = manager;
        repository.persist(e);
        return e;
    }
}