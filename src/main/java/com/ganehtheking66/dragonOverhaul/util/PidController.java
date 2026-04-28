package com.ganehtheking66.dragonOverhaul.util;

/**
 * A standard PID controller utility.
 *
 * @author GanehtHEkinG66
 */
public class PidController {

    private double kP;
    private double kI;
    private double kD;
    private double previousError = -1.0;
    private double totalError = -1.0;

    public PidController(double kP, double kI, double kD) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
    }

    /**
     * Calculates the next output of the PID controller.
     *
     * @param measurement the current state
     * @param setpoint    the desired state
     * @return the calculated output
     */
    public double calculate(double measurement, double setpoint) {
        double error = setpoint - measurement;

        if(this.previousError == -1.0) {
            this.previousError = error;
            this.totalError = 0.0;
        }

        this.totalError += error;
        double p = this.kP * error;
        double i = this.kI * this.totalError;
        double d = this.kD * (error - this.previousError);

        this.previousError = error;
        return p + i + d;
    }

    /**
     * Resets the controller error states.
     */
    public void reset() {
        this.previousError = -1.0;
        this.totalError = -1.0;
    }
}
