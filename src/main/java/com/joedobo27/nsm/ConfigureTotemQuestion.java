package com.joedobo27.nsm;

import com.joedobo27.libs.BmlForm;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.Question;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestion;

import java.util.Properties;
import java.util.logging.Level;


public class ConfigureTotemQuestion implements ModQuestion {
    private Item totem;
    private int zoneRadius;
    private int questionType;

    ConfigureTotemQuestion(Creature responder, String title, String question, int type, Item totem) {
        this.questionType = type;
        this.totem = totem;
        this.zoneRadius = Totem.decodeZoneRadius(totem);
    }

    @Override
    public int getType() {
        return this.questionType;
    }

    @Override
    public void answer(Question question, Properties answer) {
        if (question.getType() == 0) {
            NatureSwayingMod.logger.log(Level.INFO, "Received answer for a question with NOQUESTION.");
            return;
        }
        if (this.getType() == question.getType()) {
            boolean radiusBox = Boolean.parseBoolean(answer.getProperty("radiusBox"));
            if (radiusBox) {
                this.zoneRadius = Integer.parseInt(answer.getProperty("radiusValue"));
            }
            if (radiusBox){
                if (this.totem.getData1() == -1){
                    this.totem.setData1(0);
                }
                Totem.encodeZoneRadius(this.totem, this.zoneRadius);
            }
        }
    }

    @Override
    public void sendQuestion(Question question) {
        BmlForm bmlForm = new BmlForm(question.getTitle(), 300, 150);
        bmlForm.addHidden("id", Integer.toString(question.getId()));
        bmlForm.beginTable(3, 3,
                "label{text=\" \"};text{type=\"bold\";text=\"Input\"};text{width=\"100\";type=\"bold\";text=\"Current setting.\"};");
        bmlForm.addCheckBox("radiusBox", false);
        bmlForm.addInput("radiusValue", Integer.toString(this.zoneRadius), 20);
        bmlForm.addLabel(String.format("radiusValue of %1$s.            ", Integer.toString(this.zoneRadius)));
        bmlForm.endTable();
        bmlForm.addButton("Send", "submit");
        String bml = bmlForm.toString();
        //logger.log(Level.INFO, bml);
        question.getResponder().getCommunicator().sendBml(300, 150, true, true,
                bml, 200, 200, 200, question.getTitle());
    }


}
