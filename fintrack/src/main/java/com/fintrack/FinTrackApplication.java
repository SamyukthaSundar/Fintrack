package com.fintrack;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FinTrack - Collaborative Expense Engine
 * UE23CS352B OOAD Mini Project
 *
 * Team:
 *  - Saanvi Kakkar   : Identity & Optimization Lead (Debt Simplification + User/Group)
 *  - Samyuktha S     : Automation & Strategy Lead   (OCR Expenses + Split Logic)
 *  - Sanika Gupta    : Settlement & Analytics Lead  (Settlements + Analytics/Reports)
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class FinTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinTrackApplication.class, args);
        System.out.println("""
                
                ╔═══════════════════════════════════════════╗
                ║   FinTrack - Collaborative Expense Engine ║
                ║   UE23CS352B  |  Spring Boot 3.2          ║
                ║   Running at: http://localhost:8080        ║
                ╚═══════════════════════════════════════════╝
                """);
    }
}
