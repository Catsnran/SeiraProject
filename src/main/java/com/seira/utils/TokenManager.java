package com.seira.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.io.*;
import java.util.Date;


public class TokenManager {
    private static final String TOKEN_FILE = "data/token.dat";
    
    // JJWT 0.12.3 requires at least a 256-bit (32-byte) key for HMAC-SHA algorithms
    private static final String ENCRYPTION_KEY = "seiraSecretKey123456789012345678"; 
    private static final SecretKey KEY = Keys.hmacShaKeyFor(ENCRYPTION_KEY.getBytes());
    
    public static void saveToken(String user) throws Exception {
        String token = Jwts.builder()
            .subject(user)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 300000))
            .signWith(KEY)
            .compact();
        
        new File("data").mkdirs();
        
        try (FileWriter writer = new FileWriter(TOKEN_FILE)) {
            writer.write(token);
        }
    }
    
    public static String loadToken() throws Exception {
        File file = new File(TOKEN_FILE);
        if (!file.exists()) return null;
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        
        String token = content.toString();
        try {
            return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        } catch (Exception e) {
            return null;
        }
    }
    
    public static void deleteToken() {
        new File(TOKEN_FILE).delete();
    }
}