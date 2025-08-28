package net.brodino.petsinabag.network;

import net.brodino.petsinabag.PetsInABag;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class NetworkHandler {
    
    public static final Identifier SUMMON_PET_PACKET = new Identifier(PetsInABag.MOD_ID, "summon_pet");
    public static final Identifier RELEASE_PET_PACKET = new Identifier(PetsInABag.MOD_ID, "release_pet");
    public static final Identifier RECALL_PET_PACKET = new Identifier(PetsInABag.MOD_ID, "recall_pet");
    public static final Identifier SYNC_SUMMONED_PETS_PACKET = new Identifier(PetsInABag.MOD_ID, "sync_summoned_pets");
    
    public static void initialize() {
        ServerPlayNetworking.registerGlobalReceiver(SUMMON_PET_PACKET, PetOperationPacketHandler::handleSummonPet);
        ServerPlayNetworking.registerGlobalReceiver(RELEASE_PET_PACKET, PetOperationPacketHandler::handleReleasePet);
        ServerPlayNetworking.registerGlobalReceiver(RECALL_PET_PACKET, PetOperationPacketHandler::handleRecallPet);
    }
}