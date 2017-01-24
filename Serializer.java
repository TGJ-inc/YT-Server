/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package yt_server_side;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import yt_client_side.Client_Profile;

/**
 *
 * @author Your Name
 * <Tristan Janicki, Teacher: Anthony Ganuelas, Last Edited: , Date Created, Program Description>
 */
public class Serializer implements Runnable {

    Client_Profile cp;

    public Serializer(Client_Profile cp) {
        this.cp = cp;
    }

    @Override
    public void run() {
        if(serialize_CP(cp)){
            System.out.println("Successfully serialized " + cp.getCid());
        }else{
            System.out.println("Failed to serialize " + cp.getCid());
        }
    }

    /**
     *
     * @param cp - client profile to be serialized
     * @return
     */
    public boolean serialize_CP(Client_Profile cp) {

        String suffix = "\\" + cp.getCid() + ".ser";
        try (FileOutputStream fo = new FileOutputStream(YT_Server_Side.serverLocal + suffix); 
                ObjectOutputStream ou = new ObjectOutputStream(fo);) {

            ou.writeObject(cp);

            ou.flush();
            fo.flush();

        } catch (IOException ex) {
            ex.printStackTrace(System.out);
            return false;
        }
        return true;
    }

}
