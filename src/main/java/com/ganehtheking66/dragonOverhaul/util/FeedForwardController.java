package com.ganehtheking66.dragonOverhaul.util;

/**
 * A standard feedforward controller utility.
 *
 * @author GanehtHEkinG66
 */
public class FeedForwardController {

    private final double kS;
    private final double kV;
    private final double kA;

    public FeedForwardController(double kS, double kV, double kA) {
        this.kS = kS;
        this.kV = kV;
        this.kA = kA;
    }

    /**
     * Calculates the feedforward output for a desired velocity.
     *
     * @param targetVelocity the desired velocity
     * @return the calculated feedforward output
     */
    public double calculate(double targetVelocity) {
        double staticFeedForward = 0.0;

        if(targetVelocity > 0.0) {
            staticFeedForward = this.kS;
        }

        if(targetVelocity < 0.0) {
            staticFeedForward = -this.kS;
        }

        return staticFeedForward + this.kV * targetVelocity;
    }

    /**
     * Calculates the feedforward output for a desired velocity and acceleration.
     *
     * @param targetVelocity     the desired velocity
     * @param targetAcceleration the desired acceleration
     * @return the calculated feedforward output
     */
    public double calculate(double targetVelocity, double targetAcceleration) {
        return this.calculate(targetVelocity) + this.kA * targetAcceleration;
    }
}
