package com.ai.docreader.entity;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	private String email;
	
	private String password;
	
	private String name;
	
	private String role;
	
	private Timestamp createdDt;
	 
    public User(String email, String password, String name) {
        this.email    = email;
        this.password = password;
        this.name     = name;
        this.role     = "ROLE_USER";
    }
	
	@PrePersist
	protected void onCreate() {
		if (role == null) {
			role = "ROLE_USER";
		}
		createdDt = new Timestamp(System.currentTimeMillis());
	}
	

}
