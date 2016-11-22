package de.js_jabs.lateinloesungen;

/**
 * Created by Janik on 21.11.2015.
 */
public class Lektion {
    public int id;
    public String data;
    public Vokablel[] vokablels;

    public Lektion(int id){
        this.id = id;
    }

    public void setData(String data){
        this.data = data;
    }

    public void setVoc(String vocData){
        String[] allVoc = vocData.split("NEWVOC");
        boolean selector = true;
        int counter = 0;
        for(String voc : allVoc){
            if (selector == true){
                selector = false;
            }else {
                selector = true;
                counter++;
            }
        }
        vokablels = new Vokablel[counter];
        for(int i = 0; i < vokablels.length; i++){
            vokablels[i] = new Vokablel();
        }
        selector = true;
        counter = 0;
        if(vokablels.length > 0){
            for(int i = 0; i < allVoc.length; i++){
                if (selector == true){
                    vokablels[counter].latein = allVoc[i];
                    selector = false;
                }else {
                    vokablels[counter].deutsch = allVoc[i];
                    selector = true;
                    counter++;
                }
            }
        }
    }
}
