package health

import (
	"context"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/redis/go-redis/v9"
)

type Handler struct {
	redisClient *redis.Client
	version     string
}

func NewHandler(redisClient *redis.Client, version string) *Handler {
	return &Handler{redisClient: redisClient, version: version}
}

func (h *Handler) RegisterRoutes(r *gin.Engine) {
	r.GET("/health", h.health)
	r.GET("/info", h.info)
}

func (h *Handler) health(c *gin.Context) {
	status := gin.H{"status": "UP"}
	httpStatus := http.StatusOK

	if h.redisClient != nil {
		redisStatus := h.redisHealth()
		status["components"] = gin.H{
			"redis": gin.H{"status": redisStatus},
		}
		if redisStatus != "UP" {
			status["status"] = "DOWN"
			httpStatus = http.StatusServiceUnavailable
		}
	}

	c.JSON(httpStatus, status)
}

func (h *Handler) redisHealth() string {
	result, err := h.redisClient.Ping(context.Background()).Result()
	if err != nil || result != "PONG" {
		return "DOWN"
	}
	return "UP"
}

func (h *Handler) info(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"app": gin.H{"version": h.version},
	})
}
