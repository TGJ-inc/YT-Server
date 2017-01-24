package main;

import yt_server_side.YT_Server_Side;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Your Name
 * <Tristan Janicki, Teacher: Anthony Ganuelas, Last Edited: , Date Created, Program Description>
 */
public class Server_Main {

    public static void main(String[] args) {
        System.out.println("Server test");
        YT_Server_Side ys = new YT_Server_Side();

//        Client_Profile cp = new Client_Profile();
//
//        ys.getRecommendedPlaylistTiles(""https://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI"")
//          .forEach(tile -> {
//            cp.addRecomendations(tile);
//        });
//        
//        ys.serialize_CP(cp);

        new Thread(ys).start();
    }
}
