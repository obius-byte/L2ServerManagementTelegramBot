package info.mmo_dev.telegram.bot.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @see <a href="https://core.telegram.org/bots/api#inlinekeyboardmarkup">InlineKeyboardMarkup</a>
 */
public class InlineKeyboardMarkup {
    public List<List<InlineKeyboardButton>> inline_keyboard = new ArrayList<>();

    public void addRow(List<InlineKeyboardButton> cols) {
        inline_keyboard.add(cols);
    }

    public void addRow(int index, List<InlineKeyboardButton> cols) {
        inline_keyboard.add(index, cols);
    }

    public void addCol(int rowIndex, InlineKeyboardButton inlineKeyboardButton) {
        inline_keyboard.get(rowIndex).add(inlineKeyboardButton);
    }
}
