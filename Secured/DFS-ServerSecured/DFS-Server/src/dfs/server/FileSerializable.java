/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dfs.server;

import java.io.Serializable;
import java.util.Date;

/**
 * Carries file data over RMI.
 * Copy this file into BOTH Server and Client projects.
 */
public class FileSerializable implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private byte[] data;
    private Date lastModifiedDate;

    // NetBeans: Alt+Insert → Getter and Setter to generate these
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public Date getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(Date lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
}