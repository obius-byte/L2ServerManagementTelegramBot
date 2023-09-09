package info.mmo_dev.telegram.bot.api.model;

import java.util.List;

/**
 * @see <a href="https://core.telegram.org/bots/api#encryptedpassportelement">EncryptedPassportElement</a>
 */
public class EncryptedPassportElement {
    public String type;

    public String data;

    public String phone_number;

    public String email;

    public List<PassportFile> files;

    public PassportFile front_side;

    public PassportFile reverse_side;

    public PassportFile selfie;

    public List<PassportFile> translation;

    public String hash;
}
