package backend

import (
	"fmt"
	"strings"
	"time"

	"github.com/immesys/spawnpoint/objects"
	"github.com/mgutz/ansi"
)

func PrintLogMsg(logMsg *objects.SPLogMsg) {
	tstring := time.Unix(0, logMsg.Time).Format("01/02 15:04:05")
	fmt.Printf("[%s] %s%s::%s > %s%s\n", tstring, ansi.ColorCode("blue+b"), logMsg.SPAlias,
		logMsg.Service, ansi.ColorCode("reset"), strings.Trim(logMsg.Contents, "\n"))
}
