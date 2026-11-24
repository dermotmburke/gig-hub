{{- define "gig-hub.labels" -}}
app.kubernetes.io/name: gig-hub
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
{{- end }}

{{- define "gig-hub.flaresolverr.name" -}}
{{ .Release.Name }}-flaresolverr
{{- end }}

{{- define "gig-hub.flaresolverr.selectorLabels" -}}
app.kubernetes.io/name: flaresolverr
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
