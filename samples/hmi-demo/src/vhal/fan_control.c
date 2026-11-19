/* Illustrative vehicle-side handler: shows that the trace tag works in C comments too. */
#include <stdint.h>

#define FAN_MIN 1
#define FAN_MAX 6

/* @Requirement SW-HMI-003 */
int32_t clamp_fan_level(int32_t requested) {
    if (requested < FAN_MIN) return FAN_MIN;
    if (requested > FAN_MAX) return FAN_MAX;
    return requested;
}
