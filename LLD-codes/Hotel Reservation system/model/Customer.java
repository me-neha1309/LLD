/*
 * Customer entity representing a guest
 */
package  model;
public class Customer {
    private final Long id;
    private final String name;
    private final String email;

    public Customer(Long id, String name, String email){
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }
}