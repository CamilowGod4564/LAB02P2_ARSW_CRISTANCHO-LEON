package edu.eci.arsw.blacklistvalidator;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        HostBlackListsValidator validator = new HostBlackListsValidator();
        List<Integer> occurrences = validator.checkHost("200.24.34.55");
        System.out.println("The host was found in the following blacklists: " + occurrences);
    }
}