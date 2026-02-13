package com.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuthorService {

    // No @Inject needed — the metamodel accessor gives you the repo directly
    public void printAllBritish() {
        Author_.repo()
                .findByCountry("UK")
                .forEach(a -> System.out.println(a.name));
    }

    // More realistic: use it in a method that already has a transaction
    @Transactional
    public long promoteBritishAuthors() {
        return Author_.repo()
                .findByCountry("UK")
                .stream()
                .peek(a -> a.name = a.name + " (Featured)")
                .count();
    }

    // Also handy in stream pipelines where injection is awkward
    @Transactional
    public void transferAuthors(String fromCountry, String toCountry) {
        Author_.repo()
                .findByCountry(fromCountry)
                .forEach(a -> a.country = toCountry);
        // managed session dirty-checks the changes — no explicit update() needed
    }
}