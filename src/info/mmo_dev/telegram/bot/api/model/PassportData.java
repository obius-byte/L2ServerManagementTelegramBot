package info.mmo_dev.telegram.bot.api.model;

import java.util.List;

/**
 * @see <a href="https://core.telegram.org/bots/api#passportdata">PassportData</a>
 */
public class PassportData {
    public List<EncryptedPassportElement> data;

    public EncryptedCredentials credentials;
}
