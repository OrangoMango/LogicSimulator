package com.orangomango.logicsim.core;

import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

import com.orangomango.logicsim.Util;

public interface DelayedGate{
	public boolean getLastValue();
	public void setLastValue(boolean v);

	public default void applyValue(BooleanSupplier value, Consumer<Boolean> consumer){
		if (value.getAsBoolean() != getLastValue()){
			Util.schedule(() -> consumer.accept(value.getAsBoolean()), Util.GATE_DELAY);
			setLastValue(value.getAsBoolean());
		}
	}
}