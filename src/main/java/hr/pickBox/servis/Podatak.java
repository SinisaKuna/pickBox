package hr.pickBox.servis;

public class Podatak<T> {

    private String naziv;
    private String polje;
    private String model;
    private String id;
    private String opcija;



    public Podatak(String naziv, String polje, String model, String id, String opcija) {
        this.naziv = naziv;
        this.polje = polje;
        this.model = model;
        this.id = id;
        this.opcija = opcija;


    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getPolje() {
        return polje;
    }

    public void setPolje(String polje) {
        this.polje = polje;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOpcija() {
        return opcija;
    }

    public void setOpcija(String opcija) {
        this.opcija = opcija;
    }
}
