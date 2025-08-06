package com.example.testutils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class SFTPTestUtility {
    private SshServer sshServer;
    private JSch jsch;
    private Session session;
    private ChannelSftp channelSftp;
    
    private String host = "localhost";
    private int port = 2222;
    private String username = "testuser";
    private String password = "testpass";
    private String homeDirectory;
    private File tempDirectory;
    
    public SFTPTestUtility() throws IOException {
        this("localhost", 2222, "testuser", "testpass");
    }
    
    public SFTPTestUtility(String host, int port, String username, String password) throws IOException {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        
        tempDirectory = Files.createTempDirectory("sftp-test-").toFile();
        tempDirectory.deleteOnExit();
        homeDirectory = tempDirectory.getAbsolutePath();
    }
    
    public void setup() throws Exception {
        setupServer();
        setupClient();
    }
    
    private void setupServer() throws Exception {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);
        sshServer.setHost(host);
        
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(
            Paths.get(homeDirectory, "hostkey.ser")));
        
        sshServer.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String username, String password, ServerSession session) {
                return SFTPTestUtility.this.username.equals(username) && 
                       SFTPTestUtility.this.password.equals(password);
            }
        });
        
        sshServer.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(homeDirectory)));
        sshServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        
        sshServer.start();
        
        Thread.sleep(1000);
    }
    
    private void setupClient() throws Exception {
        jsch = new JSch();
        
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        
        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig(config);
        session.connect();
        
        Channel channel = session.openChannel("sftp");
        channel.connect();
        channelSftp = (ChannelSftp) channel;
    }
    
    public void uploadFile(String localFilePath, String remoteFilePath) throws Exception {
        if (channelSftp == null) {
            throw new Exception("SFTP channel not established. Call setup() first.");
        }
        
        try {
            channelSftp.put(localFilePath, remoteFilePath);
        } catch (SftpException e) {
            throw new Exception("Failed to upload file: " + e.getMessage(), e);
        }
    }
    
    public void uploadFileContent(String content, String remoteFilePath) throws Exception {
        File tempFile = File.createTempFile("sftp-upload-", ".tmp");
        tempFile.deleteOnExit();
        
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(content.trim().getBytes());
        }
        
        uploadFile(tempFile.getAbsolutePath(), remoteFilePath);
    }
    
    public void uploadFileFromTextFile(String textFilePath, String remoteFilePath) throws Exception {
        String content = readFileContent(textFilePath);
        uploadFileContent(content, remoteFilePath);
    }
    
    public void downloadFile(String remoteFilePath, String localFilePath) throws Exception {
        if (channelSftp == null) {
            throw new Exception("SFTP channel not established. Call setup() first.");
        }
        
        try {
            channelSftp.get(remoteFilePath, localFilePath);
        } catch (SftpException e) {
            throw new Exception("Failed to download file: " + e.getMessage(), e);
        }
    }
    
    public String downloadFileContent(String remoteFilePath) throws Exception {
        File tempFile = File.createTempFile("sftp-download-", ".tmp");
        tempFile.deleteOnExit();
        
        downloadFile(remoteFilePath, tempFile.getAbsolutePath());
        
        return readFileContent(tempFile.getAbsolutePath()).trim();
    }
    
    public boolean fileExists(String remoteFilePath) throws Exception {
        if (channelSftp == null) {
            throw new Exception("SFTP channel not established. Call setup() first.");
        }
        
        try {
            channelSftp.lstat(remoteFilePath);
            return true;
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            }
            throw new Exception("Failed to check file existence: " + e.getMessage(), e);
        }
    }
    
    public void deleteFile(String remoteFilePath) throws Exception {
        if (channelSftp == null) {
            throw new Exception("SFTP channel not established. Call setup() first.");
        }
        
        try {
            channelSftp.rm(remoteFilePath);
        } catch (SftpException e) {
            throw new Exception("Failed to delete file: " + e.getMessage(), e);
        }
    }
    
    public void createDirectory(String remoteDirPath) throws Exception {
        if (channelSftp == null) {
            throw new Exception("SFTP channel not established. Call setup() first.");
        }
        
        try {
            channelSftp.mkdir(remoteDirPath);
        } catch (SftpException e) {
            if (e.id != ChannelSftp.SSH_FX_FAILURE) {
                throw new Exception("Failed to create directory: " + e.getMessage(), e);
            }
        }
    }
    
    public void removeDirectory(String remoteDirPath) throws Exception {
        if (channelSftp == null) {
            throw new Exception("SFTP channel not established. Call setup() first.");
        }
        
        try {
            channelSftp.rmdir(remoteDirPath);
        } catch (SftpException e) {
            throw new Exception("Failed to remove directory: " + e.getMessage(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> listFiles(String remoteDirPath) throws Exception {
        if (channelSftp == null) {
            throw new Exception("SFTP channel not established. Call setup() first.");
        }
        
        List<String> files = new ArrayList<>();
        
        try {
            List<ChannelSftp.LsEntry> entries = channelSftp.ls(remoteDirPath);
            for (ChannelSftp.LsEntry entry : entries) {
                String filename = entry.getFilename();
                if (!".".equals(filename) && !"..".equals(filename)) {
                    files.add(filename);
                }
            }
        } catch (SftpException e) {
            throw new Exception("Failed to list files: " + e.getMessage(), e);
        }
        
        return files;
    }
    
    public long getFileSize(String remoteFilePath) throws Exception {
        if (channelSftp == null) {
            throw new Exception("SFTP channel not established. Call setup() first.");
        }
        
        try {
            return channelSftp.lstat(remoteFilePath).getSize();
        } catch (SftpException e) {
            throw new Exception("Failed to get file size: " + e.getMessage(), e);
        }
    }
    
    public void changeDirectory(String remoteDirPath) throws Exception {
        if (channelSftp == null) {
            throw new Exception("SFTP channel not established. Call setup() first.");
        }
        
        try {
            channelSftp.cd(remoteDirPath);
        } catch (SftpException e) {
            throw new Exception("Failed to change directory: " + e.getMessage(), e);
        }
    }
    
    public String getCurrentDirectory() throws Exception {
        if (channelSftp == null) {
            throw new Exception("SFTP channel not established. Call setup() first.");
        }
        
        try {
            return channelSftp.pwd();
        } catch (SftpException e) {
            throw new Exception("Failed to get current directory: " + e.getMessage(), e);
        }
    }
    
    private String readFileContent(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        // Try to load as resource first, then as file
        java.io.InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (inputStream != null) {
            // Load from resource
            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
            }
        } else {
            // If resource not found, try as file path
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
            }
        }
        return content.toString().trim();
    }
    
    public void teardown() {
        try {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
                channelSftp = null;
            }
            
            if (session != null && session.isConnected()) {
                session.disconnect();
                session = null;
            }
            
            if (sshServer != null) {
                sshServer.stop();
                sshServer = null;
            }
            
            if (tempDirectory != null && tempDirectory.exists()) {
                deleteDirectoryRecursively(tempDirectory);
            }
        } catch (Exception e) {
            System.err.println("Error during SFTP teardown: " + e.getMessage());
        }
    }
    
    private void deleteDirectoryRecursively(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
    
    public boolean isConnected() {
        return channelSftp != null && channelSftp.isConnected() && 
               session != null && session.isConnected();
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getHomeDirectory() {
        return homeDirectory;
    }
}