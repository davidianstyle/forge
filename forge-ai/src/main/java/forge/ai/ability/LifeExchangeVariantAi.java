package forge.ai.ability;

import forge.ai.*;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.MagicStack;
import forge.util.MyRandom;

public class LifeExchangeVariantAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI
     * (forge.game.player.Player, java.util.Map,
     * forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final Game game = ai.getGame();

        if ("Tree of Redemption".equals(sourceName)) {
            if (!ai.canGainLife())
                return false;

            // someone controls "Rain of Gore" or "Sulfuric Vortex", lifegain is bad in that case
            if (game.isCardInPlay("Rain of Gore") || game.isCardInPlay("Sulfuric Vortex"))
                return false;

            // an opponent controls "Tainted Remedy", lifegain is bad in that case
            for (Player op : ai.getOpponents()) {
                if (op.isCardInPlay("Tainted Remedy"))
                    return false;
            }

            if (ComputerUtil.waitForBlocking(sa) || ai.getLife() + 1 >= source.getNetToughness()
                || (ai.getLife() > 5 && !ComputerUtilCombat.lifeInSeriousDanger(ai, ai.getGame().getCombat()))) {
                return false;
            }
        }
        else if ("Tree of Perdition".equals(sourceName)) {
            boolean shouldDo = false;

            if (ComputerUtil.waitForBlocking(sa))
                return false;

            for (Player op : ai.getOpponents()) {
                // if oppoent can't be targeted, or it can't lose life, try another one
                if (!op.canBeTargetedBy(sa) || !op.canLoseLife())
                    continue;
                // an opponent has more live than this toughness
                if (op.getLife() + 1 >= source.getNetToughness()) {
                    shouldDo = true;
                } else {
                    // opponent can't gain life, so "Tainted Remedy" should not work.
                    if (!op.canGainLife()) {
                        continue;
                    } else if (ai.isCardInPlay("Tainted Remedy")) { // or AI has Tainted Remedy 
                        shouldDo = true;
                    } else {
                        for (Player ally : ai.getAllies()) {
                            // if an Ally has Tainted Remedy and opponent is also opponent of ally
                            if (ally.isCardInPlay("Tainted Remedy") && op.isOpponentOf(ally))
                                shouldDo = true;
                        }
                    }

                }

                if (shouldDo) {
                    sa.getTargets().add(op);
                    break;
                }
            }

            return shouldDo;
        }
        else if ("Evra, Halcyon Witness".equals(sourceName)) {
            if (!ai.canGainLife())
                return false;

            int aiLife = ai.getLife();

            if (source.getNetPower() > aiLife) {
                if (ComputerUtilCombat.lifeInSeriousDanger(ai, game.getCombat())) {
                    return true;
                }

                // check the top of stack
                MagicStack stack = game.getStack();
                if (!stack.isEmpty()) {
                    SpellAbility saTop = stack.peekAbility();
                    if (ComputerUtil.predictDamageFromSpell(saTop, ai) >= aiLife) {
                        return true;
                    }
                }
            }

            if (game.getCombat() != null && game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    && game.getCombat().isAttacking(source) && source.getNetPower() > 0
                    && source.getNetPower() < ai.getLife()) {
                Player def = game.getCombat().getDefenderPlayerByAttacker(source);
                if (game.getCombat().isUnblocked(source) && def.canLoseLife() && ai.getLife() >= def.getLife() && source.getNetPower() < def.getLife()) {
                    // Unblocked Evra which can deal lethal damage
                    return true;
                } else if (ai.getLife() > source.getNetPower()) {
                    int dangerMin = (((PlayerControllerAi) ai.getController()).getAi().getIntProperty(AiProps.AI_IN_DANGER_THRESHOLD));
                    int dangerMax = (((PlayerControllerAi) ai.getController()).getAi().getIntProperty(AiProps.AI_IN_DANGER_MAX_THRESHOLD));
                    int dangerDiff = dangerMax - dangerMin;
                    int lifeInDanger = dangerDiff == 0 ? dangerMin : MyRandom.getRandom().nextInt(dangerDiff) + dangerMin;
                    if (source.getNetPower() >= lifeInDanger) {
                        // Blocked or unblocked Evra which will get bigger *and* we're getting our life back through Lifelink
                        return true;
                    }
                }
            }
        }
        return false;

    }

    /**
     * <p>
     * exchangeLifeDoTriggerAINoCost.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     *
     * @return a boolean.
     */
    @Override
    protected boolean doTriggerAINoCost(final Player ai, final SpellAbility sa,
    final boolean mandatory) {

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        Player opp = ComputerUtil.getOpponentFor(ai);
        if (tgt != null) {
            sa.resetTargets();
            if (sa.canTarget(opp) && (mandatory || ai.getLife() < opp.getLife())) {
                sa.getTargets().add(opp);
            } else {
                return false;
            }
        }
        return true;
    }

}
